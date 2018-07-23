package se.l4.silo.engine.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import se.l4.silo.FetchResult;
import se.l4.silo.Silo;
import se.l4.silo.engine.Index;
import se.l4.silo.engine.LocalSilo;
import se.l4.silo.query.IndexQuery;
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
				.add("byName", Index::queryEngine)
					.addField("name")
					.done()
				.add("byAge", Index::queryEngine)
					.addField("age")
					.addSortField("name")
					.done()
				.done()
			.build();

		entity = silo.structured("test").asObject(TestUserData.class, TestUserData::getId);
	}

	@After
	public void after()
		throws Exception
	{
		silo.close();
		DataUtils.removeRecursive(tmp);
	}

	@Test
	public void testStoreNoTransaction()
	{
		TestUserData obj = new TestUserData(2, "Donna Johnson", 28, false);
		entity.store(obj);

		Optional<TestUserData> fetched = entity.get(2);

		Assert.assertEquals(obj, fetched.get());
	}

	@Test
	public void testQuery()
	{
		for(int i=0; i<1000; i++)
		{
			entity.store(new TestUserData(i, i % 2 == 0 ? "Donna" : "Eric", 18 + i % 40, i % 2 == 0));
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
