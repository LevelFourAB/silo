package se.l4.silo.search;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import se.l4.silo.Silo;
import se.l4.silo.engine.LocalSilo;
import se.l4.silo.engine.internal.DataUtils;
import se.l4.silo.engine.internal.TestUserData;
import se.l4.silo.engine.search.SearchFields;
import se.l4.silo.engine.search.SearchIndex;
import se.l4.silo.engine.search.facets.category.CategoryFacet;
import se.l4.silo.search.facet.CategoryFacetQuery;
import se.l4.silo.structured.ObjectEntity;

public class SearchObjectEntityTest
{
	private ObjectEntity<TestUserData> entity;
	private Path tmp;
	private Silo silo;

	@Before
	public void before()
		throws IOException
	{
		tmp = Files.createTempDirectory("silo");
		silo = LocalSilo.open(tmp)
			.addQueryEngine(SearchIndex.builder().build())
			.addEntity("test")
				.asStructured()
				.defineField("name", "string")
				.defineField("age", "int")
				.defineField("active", "boolean")
				.add("index", SearchIndex::queryEngine)
					.addField("name").type(SearchFields.TEXT).done()
					.addField("age").type(SearchFields.INTEGER).done()
					.addField("active").type(SearchFields.BOOLEAN).done()
					.addFacet("ageFacet", CategoryFacet::newFacet)
						.setField("age")
						.done()
					.done()
				.done()
			.build();

		entity = silo.structured("test")
			.asObject(TestUserData.class, TestUserData::getId);
	}

	@After
	public void after()
		throws Exception
	{
		silo.close();
		DataUtils.removeRecursive(tmp);
	}

	@Test
	public void testQuery()
	{
		for(int i=0; i<1000; i++)
		{
			entity.store(new TestUserData(i, i % 2 == 0 ? "Donna" : "Eric", 18 + i % 40, i % 2 == 0));
		}

		try
		{
			Thread.sleep(1000);
		}
		catch(InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try(SearchResult<TestUserData> fr = entity.query("index", SearchIndexQuery.type())
			.user("name").text("donna")
			.number("age").range(18, 21)
			.withFacet("ageFacet", CategoryFacetQuery::new)
				.done()
			.run())
		{
			assertThat(fr.getSize(), is(10l));
			assertThat(fr.getTotal(), is(50l));
		}
	}

	@Test
	public void testWaitForLatest()
	{
		for(int i=0; i<1000; i++)
		{
			entity.store(new TestUserData(i, i % 2 == 0 ? "Donna" : "Eric", 18 + i % 40, i % 2 == 0));
		}

		try(SearchResult<TestUserData> fr = entity.query("index", SearchIndexQuery.type())
			.waitForLatest()
			.user("name").text("donna")
			.number("age").range(18, 21)
			.withFacet("ageFacet", CategoryFacetQuery::new)
				.done()
			.run())
		{
			assertThat(fr.getSize(), is(10l));
			assertThat(fr.getTotal(), is(50l));
		}
	}

	@Test
	public void testNull()
	{
		for(int i=0; i<1000; i++)
		{
			entity.store(new TestUserData(i, i % 2 == 0 ? "Donna" : null, 18 + i % 40, i % 2 == 0));
		}

		try
		{
			Thread.sleep(1000);
		}
		catch(InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try(SearchResult<TestUserData> fr = entity.query("index", SearchIndexQuery.type())
			.field("name").isEmpty()
			.number("age").range(18, 21)
			.withFacet("ageFacet", CategoryFacetQuery::new)
				.done()
			.run())
		{
			assertThat(fr.getSize(), is(10l));
			assertThat(fr.getTotal(), is(50l));
		}
	}

	@Test
	public void testNullVsEmpty()
	{
		for(int i=0; i<1000; i++)
		{
			entity.store(new TestUserData(i, i % 2 == 0 ? "" : null, 18 + i % 40, i % 2 == 0));
		}

		try
		{
			Thread.sleep(1000);
		}
		catch(InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try(SearchResult<TestUserData> fr = entity.query("index", SearchIndexQuery.type())
			.field("name").isEmpty()
			.number("age").range(18, 21)
			.withFacet("ageFacet", CategoryFacetQuery::new)
				.done()
			.run())
		{
			assertThat(fr.getSize(), is(10l));
			assertThat(fr.getTotal(), is(50l));
		}
	}
}
