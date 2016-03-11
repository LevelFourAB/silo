package se.l4.silo.engine.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import se.l4.aurochs.serialization.DefaultSerializerCollection;
import se.l4.aurochs.serialization.Expose;
import se.l4.aurochs.serialization.ReflectionSerializer;
import se.l4.aurochs.serialization.Serializer;
import se.l4.aurochs.serialization.Use;
import se.l4.silo.FetchResult;
import se.l4.silo.IndexQuery;
import se.l4.silo.Silo;
import se.l4.silo.engine.IndexQueryEngineFactory;
import se.l4.silo.engine.LocalSilo;
import se.l4.silo.structured.ObjectEntity;

public class ObjectEntityTest
{
	private ObjectEntity<Data> entity;
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
		Serializer<Data> serializer = collection.find(Data.class);
		entity = silo.structured("test").asObject(serializer);
	}
	
	public void after()
		throws Exception
	{
		silo.stop();
		DataUtils.removeRecursive(tmp);
	}
	
	@Test
	public void testStoreNoTransaction()
	{
		Data obj = new Data("Donna Johnson", 28, false);
		entity.store("test", obj);
		
		Data fetched = entity.get("test");
		
		Assert.assertEquals(obj, fetched);
	}
	
	@Test
	public void testQuery()
	{
		for(int i=0; i<1000; i++)
		{
			entity.store(i, new Data(i % 2 == 0 ? "Donna" : "Eric", 18 + i % 40, i % 2 == 0));
		}
		
		try(FetchResult<Data> fr = entity.query("byAge", IndexQuery.type())
			.field("age")
			.isMoreThan(30)
			.field("name")
			.sortAscending()
			.run())
		{
			for(Data d : fr)
			{
				if(d.age <= 30)
				{
					throw new AssertionError("Returned results with age less than or equal to 30");
				}
			}
		}
	}
	
	@Use(ReflectionSerializer.class)
	public static class Data
	{
		@Expose
		private String name;
		@Expose
		private int age;
		@Expose
		private boolean active;
		
		public Data()
		{
		}
		
		public Data(String name, int age, boolean active)
		{
			super();
			this.name = name;
			this.age = age;
			this.active = active;
		}
		
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + (active ? 1231 : 1237);
			result = prime * result + age;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj)
		{
			if(this == obj)
				return true;
			if(obj == null)
				return false;
			if(getClass() != obj.getClass())
				return false;
			Data other = (Data) obj;
			if(active != other.active)
				return false;
			if(age != other.age)
				return false;
			if(name == null)
			{
				if(other.name != null)
					return false;
			}
			else if(!name.equals(other.name))
				return false;
			return true;
		}
	}
}
