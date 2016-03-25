package se.l4.silo.engine.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import se.l4.silo.Silo;
import se.l4.silo.engine.LocalSilo;
import se.l4.silo.engine.SearchIndexQueryEngineFactory;
import se.l4.silo.search.SearchIndexQuery;
import se.l4.silo.search.SearchResult;
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
			.addEntity("test")
				.asStructured()
				.defineField("name", "string")
				.defineField("age", "int")
				.defineField("active", "boolean")
				.add("index", SearchIndexQueryEngineFactory.type())
					.addField("name")
					.addField("age")
					.addField("active")
					.done()
				.done()
			.build();
		
		entity = silo.structured("test").asObject(TestUserData.class);
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
			entity.store(i, new TestUserData(i % 2 == 0 ? "Donna" : "Eric", 18 + i % 40, i % 2 == 0));
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
			.run())
		{
			System.out.println(fr);
			
			System.out.println("Got " + fr.getSize() + ", total is " + fr.getTotal());
		}
	}
}
