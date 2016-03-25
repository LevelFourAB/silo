package se.l4.silo.engine.internal.search;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TrackingIndexWriter;
import org.apache.lucene.search.ControlledRealTimeReopenThread;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NRTCachingDirectory;
import org.apache.lucene.util.BytesRef;

import com.google.common.collect.ImmutableSet;

import se.l4.silo.StorageException;
import se.l4.silo.engine.DataEncounter;
import se.l4.silo.engine.QueryEncounter;
import se.l4.silo.engine.QueryEngine;
import se.l4.silo.engine.config.SearchIndexConfig;
import se.l4.silo.search.SearchIndexQueryRequest;

public class SearchIndexQueryEngine
	implements QueryEngine<SearchIndexQueryRequest>
{
	private final Directory directory;
	private final IndexWriter writer;
	private final TrackingIndexWriter tiw;
	private final SearcherManager manager;
	
	private final ImmutableSet<String> fieldNames;
	private ControlledRealTimeReopenThread<IndexSearcher> thread;

	public SearchIndexQueryEngine(Path directory, SearchIndexConfig config)
		throws IOException
	{
		// Create the directory implementation to use
		this.directory = createDirectory(FSDirectory.open(directory), config);
		
		// Setup a basic configuration for the index writer
		IndexWriterConfig conf = new IndexWriterConfig(new SimpleAnalyzer());
		conf.setSimilarity(new BM25Similarity());
		
		// Create the writer and searcher manager
		writer = new IndexWriter(this.directory, conf);
		tiw = new TrackingIndexWriter(writer);
		
		manager = new SearcherManager(writer, true, new SearcherFactory()
		{
			@Override
			public IndexSearcher newSearcher(IndexReader reader, IndexReader previousReader)
				throws IOException
			{
				IndexSearcher is = new IndexSearcher(reader);
				is.setSimilarity(new BM25Similarity());
				return is;
			}
		});
		

		SearchIndexConfig.Freshness freshness = config.getReload().getFreshness();
		thread = new ControlledRealTimeReopenThread<IndexSearcher>(tiw, manager, freshness.getMaxStale(), freshness.getMinStale());
		thread.setPriority(Math.min(Thread.currentThread().getPriority()+2, Thread.MAX_PRIORITY));
		thread.setDaemon(true);
		thread.start();
		
		// Figure out which fields that are going to be indexed
		ImmutableSet.Builder<String> fieldNames = ImmutableSet.builder();
		for(SearchIndexConfig.FieldConfig fc : config.getFields())
		{
			fieldNames.add(fc.getName());
		}
		
		this.fieldNames = fieldNames.build();
	}
	
	private static Directory createDirectory(Directory directory, SearchIndexConfig config)
	{
		SearchIndexConfig.Cache cache = config.getReload().getCache();
		if(cache.isActive())
		{
			return new NRTCachingDirectory(directory, cache.getMaxMergeSize(), cache.getMaxSize());
		}
		else
		{
			return directory;
		}
	}

	@Override
	public void close()
		throws IOException
	{
		thread.interrupt();
		manager.close();
		writer.close();
		directory.close();
	}

	@Override
	public void query(QueryEncounter<SearchIndexQueryRequest> encounter)
	{
		System.out.println("Should search a bit");
		IndexSearcher searcher = null;
		try
		{
			searcher = manager.acquire();
			
			TopDocs td = searcher.search(new MatchAllDocsQuery(), 10);
			encounter.setMetadata(0, 10, td.totalHits);
			
			for(ScoreDoc d : td.scoreDocs)
			{
				Document doc = searcher.doc(d.doc);
				BytesRef idRef = doc.getBinaryValue("_:id");
				
				long id = bytesToLong(idRef.bytes);
				encounter.receive(id, v -> {
					v.accept("score", d.score);
				});
			}
		}
		catch(IOException e)
		{
			throw new StorageException("Unable to search; " + e.getMessage(), e);
		}
		finally
		{
			if(searcher != null)
			{
				try
				{
					manager.release(searcher);
				}
				catch(IOException e)
				{
				}
			}
		}
	}

	@Override
	public void update(long id, DataEncounter encounter)
	{
		Document doc = new Document();
		
		BytesRef idRef = new BytesRef(longToBytes(id));
		
		FieldType ft = new FieldType();
		ft.setStored(true);
		ft.setTokenized(false);
		ft.setIndexOptions(IndexOptions.DOCS);
		doc.add(new Field("_:id", idRef, ft));
		
		encounter.findStructuredKeys(fieldNames, (k, v) -> {
			if(v == null) return;
			
			// TODO: Different types of fields
			doc.add(new StringField("f:" + k, v.toString(), Store.NO));
		});
		
		Term idTerm = new Term("_:id", idRef);
		try
		{
			tiw.updateDocument(idTerm, doc);
		}
		catch(IOException e)
		{
			throw new StorageException("Unable to update search index; " + e.getMessage(), e);
		}
	}
	

	@Override
	public void delete(long id)
	{
		BytesRef idRef = new BytesRef(longToBytes(id));
		try
		{
			tiw.deleteDocuments(new Term("_:id", idRef));
		}
		catch(IOException e)
		{
			throw new StorageException("Unable to delete from search index; " + e.getMessage(), e);
		}
	}

	private byte[] longToBytes(long id)
	{
		byte[] result = new byte[8];
		result[0] = (byte) (id >>> 56);
		result[1] = (byte) (id >>> 48);
		result[2] = (byte) (id >>> 40);
		result[3] = (byte) (id >>> 32);
        result[4] = (byte) (id >>> 24);
        result[5] = (byte) (id >>> 16);
        result[6] = (byte) (id >>> 8);
        result[7] = (byte) (id);
        return result;
	}
	
	private long bytesToLong(byte[] data)
	{
		return ((long)data[0] << 56) +
	        ((long)(data[1] & 255) << 48) +
	        ((long)(data[2] & 255) << 40) +
	        ((long)(data[3] & 255) << 32) +
	        ((long)(data[4] & 255) << 24) +
	        ((data[5] & 255) << 16) +
	        ((data[6] & 255) <<  8) +
	        ((data[7] & 255) <<  0);
	}
}
