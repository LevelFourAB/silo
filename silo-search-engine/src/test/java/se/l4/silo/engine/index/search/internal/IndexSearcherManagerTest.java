package se.l4.silo.engine.index.search.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import se.l4.silo.engine.internal.SiloTest;

public class IndexSearcherManagerTest
	extends SiloTest
{
	private IndexWriter writer;
	private IndexSearcherManager manager;

	@BeforeEach
	public void before()
		throws IOException
	{
		writer = new IndexWriter(
			FSDirectory.open(tmp),
			new IndexWriterConfig()
		);
		manager = new IndexSearcherManager(writer);
	}

	@Test
	public void testAcquireEmpty()
		throws IOException
	{
		IndexSearcherHandle handle = manager.acquire();
		assertThat(handle.getSearcher(), notNullValue());

		assertThat(manager.getHandleCount(), is(1));
		assertThat(manager.getSearcherRefCount(), is(1));

		handle.release();

		assertThat(manager.getHandleCount(), is(0));
		assertThat(manager.getSearcherRefCount(), is(1));
	}

	@Test
	public void testAcquireAfterAdd()
		throws IOException
	{
		Document doc = new Document();
		doc.add(new TextField("value", "test", Store.NO));
		writer.addDocument(doc);

		IndexSearcherHandle handle = manager.acquire();
		assertThat(handle.getSearcher(), notNullValue());

		TopDocs td = handle.getSearcher()
			.search(new TermQuery(new Term("value", "test")), 10);

		assertThat(td.scoreDocs.length, is(1));

		assertThat(manager.getHandleCount(), is(1));
		assertThat(manager.getSearcherRefCount(), is(1));

		handle.release();

		assertThat(manager.getHandleCount(), is(0));
		assertThat(manager.getSearcherRefCount(), is(1));
	}

	@Test
	public void testAcquireBeforeAdd()
		throws IOException
	{
		IndexSearcherHandle handle = manager.acquire();
		assertThat(handle.getSearcher(), notNullValue());

		Document doc = new Document();
		doc.add(new TextField("value", "test", Store.NO));
		writer.addDocument(doc);

		TopDocs td = handle.getSearcher()
			.search(new TermQuery(new Term("value", "test")), 10);

		assertThat(td.scoreDocs.length, is(0));

		assertThat(manager.getHandleCount(), is(1));
		assertThat(manager.getSearcherRefCount(), is(1));

		handle.release();

		assertThat(manager.getHandleCount(), is(0));
		assertThat(manager.getSearcherRefCount(), is(1));
	}

	@Test
	public void testWillMutateLocksVersion()
		throws IOException
	{
		IndexSearcherHandle handle = manager.acquire();

		assertThat(manager.getSearcherRefCount(), is(0));

		manager.willMutate(false);

		Document doc = new Document();
		doc.add(new TextField("value", "test", Store.NO));
		writer.addDocument(doc);

		TopDocs td = handle.getSearcher()
			.search(new TermQuery(new Term("value", "test")), 10);

		assertThat(td.scoreDocs.length, is(0));

		assertThat(manager.getHandleCount(), is(1));
		assertThat(manager.getSearcherRefCount(), is(1));

		handle.release();

		assertThat(manager.getHandleCount(), is(0));
		assertThat(manager.getSearcherRefCount(), is(1));
	}

	@Test
	public void testMultipleHandles()
		throws IOException
	{
		Document doc = new Document();
		doc.add(new TextField("value", "test", Store.NO));
		writer.addDocument(doc);

		IndexSearcherHandle h1 = manager.acquire();
		IndexSearcherHandle h2 = manager.acquire();

		manager.willMutate(false);

		assertThat(manager.getHandleCount(), is(2));
		assertThat(manager.getSearcherRefCount(), is(1));

		TopDocs td = h1.getSearcher()
			.search(new TermQuery(new Term("value", "test")), 10);

		assertThat(td.scoreDocs.length, is(1));

		td = h2.getSearcher()
			.search(new TermQuery(new Term("value", "test")), 10);

		assertThat(td.scoreDocs.length, is(1));

		h1.release();

		assertThat(manager.getHandleCount(), is(1));
		assertThat(manager.getSearcherRefCount(), is(1));

		h2.release();

		assertThat(manager.getHandleCount(), is(0));
		assertThat(manager.getSearcherRefCount(), is(1));
	}

	@Test
	public void testMultipleVersions()
		throws IOException
	{
		Document doc = new Document();
		doc.add(new TextField("value", "test", Store.NO));
		writer.addDocument(doc);

		IndexSearcherHandle h1 = manager.acquire();

		manager.willMutate(false);
		writer.addDocument(doc);

		IndexSearcherHandle h2 = manager.acquire();

		TopDocs td = h1.getSearcher()
			.search(new TermQuery(new Term("value", "test")), 10);

		assertThat(td.scoreDocs.length, is(1));

		td = h2.getSearcher()
			.search(new TermQuery(new Term("value", "test")), 10);

		assertThat(td.scoreDocs.length, is(2));

		assertThat(manager.getHandleCount(), is(2));
		assertThat(manager.getSearcherRefCount(), is(2));

		h1.release();

		assertThat(manager.getHandleCount(), is(1));
		assertThat(manager.getSearcherRefCount(), is(1));

		h2.release();

		assertThat(manager.getHandleCount(), is(0));
		assertThat(manager.getSearcherRefCount(), is(1));
	}

	@Test
	public void testDeletion()
		throws IOException
	{
		Document doc = new Document();
		doc.add(new TextField("id", "1", Store.YES));
		doc.add(new TextField("value", "test", Store.NO));

		manager.willMutate(false);
		writer.addDocument(doc);

		writer.commit();
		manager.changesCommitted();

		IndexSearcherHandle h1 = manager.acquire();

		TopDocs td = h1.getSearcher()
			.search(new TermQuery(new Term("value", "test")), 10);

		assertThat(td.scoreDocs.length, is(1));

		manager.willMutate(true);
		writer.deleteDocuments(new Term("id", "1"));

		IndexSearcherHandle h2 = manager.acquire();

		td = h2.getSearcher()
			.search(new TermQuery(new Term("value", "test")), 10);

		assertThat(td.scoreDocs.length, is(0));

		h2.release();
		h1.release();

		assertThat(manager.getHandleCount(), is(0));
		assertThat(manager.getSearcherRefCount(), is(1));
	}
}
