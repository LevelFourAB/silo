package se.l4.silo.engine.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import se.l4.aurochs.serialization.DefaultSerializerCollection;
import se.l4.aurochs.serialization.Serializer;
import se.l4.silo.FetchResult;
import se.l4.silo.IndexQuery;
import se.l4.silo.Silo;
import se.l4.silo.engine.IndexQueryEngineFactory;
import se.l4.silo.engine.LocalSilo;
import se.l4.silo.structured.ObjectEntity;

public class ObjectEntityTest
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
				.add("byName", IndexQueryEngineFactory.type())
					.addField("name")
					.done()
				.add("byAge", IndexQueryEngineFactory.type())
					.addField("age")
					.addSortField("name")
					.done()
				.done()
			.build();
		
		DefaultSerializerCollection collection = new DefaultSerializerCollection();
		Serializer<TestUserData> serializer = collection.find(TestUserData.class);
		entity = silo.structured("test").asObject(serializer);
	}
	
	@After
	public void after()
		throws Exception
	{
		silo.stop();
		DataUtils.removeRecursive(tmp);
	}
	
	@Test
	public void testStoreNoTransaction()
	{
		TestUserData obj = new TestUserData("Donna Johnson", 28, false);
		entity.store("test", obj);
		
		TestUserData fetched = entity.get("test");
		
		Assert.assertEquals(obj, fetched);
	}
	
	@Test
	public void testQuery()
	{
		for(int i=0; i<1000; i++)
		{
			entity.store(i, new TestUserData(i % 2 == 0 ? "Donna" : "Eric", 18 + i % 40, i % 2 == 0));
		}
		
		try(FetchResult<TestUserData> fr = entity.query("byAge", IndexQuery.type())
			.field("age")
			.isMoreThan(30)
			.field("name")
			.sortAscending()
			.run())
		{
			Assert.assertEquals(675, fr.getTotal());
			
			for(TestUserData d : fr)
			{
				if(d.age <= 30)
				{
					throw new AssertionError("Returned results with age less than or equal to 30");
				}
			}
		}
	}
}
