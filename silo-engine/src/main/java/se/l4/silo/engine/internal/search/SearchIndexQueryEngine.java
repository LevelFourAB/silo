package se.l4.silo.engine.internal.search;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TrackingIndexWriter;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.ControlledRealTimeReopenThread;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.TotalHitCountCollector;
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
import se.l4.silo.engine.search.FieldDefinition;
import se.l4.silo.engine.search.IndexDefinition;
import se.l4.silo.engine.search.Language;
import se.l4.silo.engine.search.query.QueryParseEncounter;
import se.l4.silo.engine.search.query.QueryParser;
import se.l4.silo.search.FacetItem;
import se.l4.silo.search.FacetsImpl;
import se.l4.silo.search.QueryItem;
import se.l4.silo.search.SearchIndexQueryRequest;

/**
 * Query engine that maintains a Lucene index for an entity.
 * 
 * @author Andreas Holstenson
 *
 */
public class SearchIndexQueryEngine
	implements QueryEngine<SearchIndexQueryRequest>
{
	private final IndexDefinitionImpl def;
	private final SearchEngine engine;
	
	private final Directory directory;
	private final IndexWriter writer;
	private final TrackingIndexWriter tiw;
	private final SearcherManager manager;
	
	private final ImmutableSet<String> fieldNames;
	private ControlledRealTimeReopenThread<IndexSearcher> thread;

	public SearchIndexQueryEngine(SearchEngine engine, Path directory, SearchIndexConfig config)
		throws IOException
	{
		this.engine = engine;
		def = new IndexDefinitionImpl(config);
		
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
		
		try
		{
			thread.join();
		}
		catch(InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public void query(QueryEncounter<SearchIndexQueryRequest> encounter)
	{
		SearchIndexQueryRequest request = encounter.getData();
		
		IndexSearcher searcher = null;
		try
		{
			searcher = manager.acquire();
			
			Collector resultCollector;
			if(request.getLimit() == 0)
			{
				resultCollector = new TotalHitCountCollector();
			}
			else if(request.getSortItems().isEmpty())
			{
				resultCollector = TopScoreDocCollector.create(request.getOffset() + request.getLimit());
			}
			else
			{
				Sort sort = createSort(request);
				resultCollector = TopFieldCollector.create(sort, request.getOffset() + request.getLimit(), false, false, false);
			}
			
			FacetsImpl facets = search(request, searcher, resultCollector, request.getLimit() > 0, true);
			
			int hits;
			if(request.getLimit() == 0)
			{
				hits = ((TotalHitCountCollector) resultCollector).getTotalHits();
			}
			else
			{
				TopDocs docs = ((TopDocsCollector<?>) resultCollector).topDocs();
				ScoreDoc[] innerDocs = docs.scoreDocs;
				if(request.getOffset() > 0)
				{
					if(innerDocs.length < request.getOffset())
					{
						innerDocs = new ScoreDoc[0];
					}
					else
					{
						innerDocs = Arrays.copyOfRange(innerDocs, request.getOffset(), Math.min(innerDocs.length, request.getOffset() + request.getLimit()));
					}
				}
				
				hits = docs.totalHits;
				
				for(ScoreDoc d : innerDocs)
				{
					Document doc = searcher.doc(d.doc);
					BytesRef idRef = doc.getBinaryValue("_:id");
					
					long id = bytesToLong(idRef.bytes);
					encounter.receive(id, v -> {
						v.accept("score", d.score);
					});
					
					// TODO: Support for highlighting
				}
			}
			
			encounter.setMetadata(request.getOffset(), request.getLimit(), hits);
			
			
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
	
	private Sort createSort(SearchIndexQueryRequest request)
	{
		SortField[] fields = request.getSortItems()
			.stream()
			.map(s -> {
				FieldDefinition fdef = def.getField(s.getField());
				return new SortField(fdef.docValuesName(null), fdef.getType().getSortType(), ! s.isAscending());
			})
			.toArray(c -> new SortField[c]);
			
		return new Sort(fields);
	}
	
	private FacetsImpl search(SearchIndexQueryRequest request, IndexSearcher searcher, Collector collector, boolean score, boolean withFacets)
		throws IOException
	{
		FacetsCollector facetsCollector = null;
		if(withFacets && ! request.getFacetItems().isEmpty())
		{
			facetsCollector = new FacetsCollector();
			collector = MultiCollector.wrap(collector, facetsCollector);
		}
		
		Query query = createQuery(request);
		
		System.out.println(query);
		/*
		if(score && request.getScoring() != null)
		{
			QueryScoringProvider provider = queryExtensions.scoring(request.getScoring());
			if(provider == null)
			{
				throw new IOException("Unknown scoring: " + request.getScoring());
			}
			
			query = new CustomScoreAdapter(request, query, provider);
		}
		*/
		
		searcher.search(query, collector);
		
		if(withFacets && ! request.getFacetItems().isEmpty())
		{
			FacetsImpl results = new FacetsImpl();
			for(FacetItem fi : request.getFacetItems())
			{
				/*
				FacetDefinition fdef = def.getFacet(fi.getId());
				if(fdef == null)
				{
					throw new IOException("Unknown facet: " + fi.getId());
				}
				
				QueryFacet qf = queryExtensions.facet(fdef.getType());
				if(qf == null)
				{
					throw new IOException("Unknown facet type: " + fdef.getType());
				}
				
				Map<String, String> params = fi.getParameters();
				List<FacetEntry> entries = qf.collect("v:" + fdef.getField() + ":_", params, searcher.getIndexReader(), facetsCollector);
				results.addAll(fi.getId(), entries);
				*/
			}
			
			return results;
		}
		
		return null;
	}
	
	private Query createQuery(SearchIndexQueryRequest request)
		throws IOException
	{
		List<QueryItem> items = request.getQueryItems();
		if(items.isEmpty())
		{
			return new MatchAllDocsQuery();
		}
		else if(items.size() == 1)
		{
			return createQuery(request, items.get(0));
		}
		else
		{
			BooleanQuery.Builder result = new BooleanQuery.Builder();
			for(QueryItem item : items)
			{
				Query q = createQuery(request, item);
				if(q instanceof BooleanQuery)
				{
					boolean hasShoulds = false;
					for(BooleanClause c : ((BooleanQuery) q).clauses())
					{
						if(c.getOccur() == BooleanClause.Occur.SHOULD)
						{
							hasShoulds = true;
							break;
						}
					}
					
					if(hasShoulds)
					{
						result.add(q, Occur.MUST);
					}
					else
					{
						for(BooleanClause c : ((BooleanQuery) q).clauses())
						{
							result.add(c);
						}
					}
				}
				else
				{
					result.add(q, Occur.MUST);
				}
			}
			
			return result.build();
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Query createQuery(SearchIndexQueryRequest request, QueryItem item)
		throws IOException
	{
		QueryParser<?> qp = engine.queryParser(item.getType());
		if(qp == null)
		{
			throw new IOException("Unknown query type: " + item.getType());
		}
		
		Language defaultLanguage = engine.getLanguage(def.getDefaultLanguage());
		Language current = request.getLanguage() == null ? defaultLanguage : engine.getLanguage(Locale.forLanguageTag(request.getLanguage()));
		return qp.parse(new QueryParseEncounter()
		{
			@Override
			public boolean isSpecificLanguage()
			{
				return current != defaultLanguage;
			}
			
			@Override
			public Language currentLanguage()
			{
				return current;
			}
			
			@Override
			public Language defaultLanguage()
			{
				return defaultLanguage;
			}
			
			@Override
			public IndexDefinition def()
			{
				return def;
			}
			
			@Override
			public Object data()
			{
				return item.getPayload();
			}
			
			@Override
			public Query parse(QueryItem item)
				throws IOException
			{
				return createQuery(request, item);
			}
		});
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
		
		// Fetch all the fields and find the language to use
		MutableString lang = new MutableString();
		List<Tuple> fields = new ArrayList<>();
		encounter.findStructuredKeys(fieldNames, (k, v) -> {
			if(v == null) return;
			
			fields.add(new Tuple(k, v));
			
			if(k.equals(def.getLanguageField()))
			{
				lang.value = v.toString();
			}
		});
		
		// Resolve the language to use
		Locale locale = null;
		if(lang.value != null)
		{
			locale = Locale.forLanguageTag(lang.value);
			if(! engine.isSupportedLanguage(locale))
			{
				// No support for the given language, just use the default
				locale = null;
			}
		}
		
		if(locale == null)
		{
			// Always use default if nothing else is available
			locale = def.getDefaultLanguage();
		}
		
		doc.add(new Field("_:lang", locale.toLanguageTag(), StringField.TYPE_STORED));
		
		Language langObj = engine.getLanguage(locale);
		Language defaultLangObj = engine.getLanguage(def.getDefaultLanguage());
		
		// Add the fields
		for(Tuple t : fields)
		{
			FieldDefinition fd = def.getField(t.key);
			addField(doc, defaultLangObj, langObj, fd, t.key, t.value);
		}
		
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
	
	private void addField(Document document, Language fallback, Language current, FieldDefinition field, String name, Object object)
	{
		if(object == null) return;
		
		if(field.isLanguageSpecific())
		{
			// Create language specific fields
			if(fallback != current)
			{
				IndexableField f1 = field.createIndexableField(name, current, object);
				document.add(f1);
			}
			
			// Add a field for the default language
			IndexableField f2 = field.createIndexableField(name, fallback, object);
			document.add(f2);
			
			if(field.isSorted())
			{
				document.add(field.createSortingField(name, null, object));
			}
		}
		else
		{
			// Create standard field
			IndexableField f2 = field.createIndexableField(name, fallback, object);
			document.add(f2);
			
			if(field.isSorted())
			{
				document.add(field.createSortingField(name, null, object));
			}
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
	
	private static class Tuple
	{
		private final String key;
		private final Object value;
		
		public Tuple(String key, Object value)
		{
			this.key = key;
			this.value = value;
		}
	}
	
	private static class MutableString
	{
		private String value;
	}
}
