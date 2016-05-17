package se.l4.silo.engine.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Throwables;

import se.l4.commons.serialization.format.BinaryInput;
import se.l4.commons.serialization.format.BinaryOutput;
import se.l4.commons.serialization.format.StreamingInput;
import se.l4.silo.FetchResult;
import se.l4.silo.Silo;
import se.l4.silo.engine.Index;
import se.l4.silo.engine.LocalSilo;
import se.l4.silo.query.IndexQuery;
import se.l4.silo.structured.StructuredEntity;

public class IndexQueryEngineTest
{
	private StructuredEntity entity;
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
				.defineField("field1")
					.collection()
					.setType("string")
					.done()
				.defineField("field2")
					.setType("boolean")
					.done()
				.add("byField1", Index::queryEngine)
					.addField("field1")
					.done()
				.add("byField2", Index::queryEngine)
					.addField("field2")
					.done()
				.add("multiple", Index::queryEngine)
					.addField("field2")
					.addField("field1")
					.done()
				.done()
			.build();
		
		entity = silo.structured("test");
	}
	
	@After
	public void after()
		throws Exception
	{
		silo.close();
		DataUtils.removeRecursive(tmp);
	}
	
	private BinaryInput generateList()
	{
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			BinaryOutput out = new BinaryOutput(baos);
			out.writeObjectStart("");
			out.writeListStart("field1");
			out.write("entry", "value1");
			out.write("entry", "value2");
			out.writeListEnd("field");
			out.write("field2", false);
			out.writeObjectEnd("");
			out.flush();
			
			return new BinaryInput(new ByteArrayInputStream(baos.toByteArray()));
		}
		catch(IOException e)
		{
			throw Throwables.propagate(e);
		}
	}
	
	@Test
	public void testStore()
	{
		entity.store("test", generateList());
		
		FetchResult<StreamingInput> fr = entity.query("byField1", IndexQuery.type())
			.field("field1")
			.isEqualTo("value1")
			.run();
		
		assertThat(fr.getSize(), is(1));
	}
	
	@Test
	public void testStoreDelete()
	{
		entity.store("test", generateList());
		
		try(FetchResult<StreamingInput> fr = entity.query("byField1", IndexQuery.type())
			.field("field1")
			.isEqualTo("value1")
			.run())
		{
			assertThat(fr.getSize(), is(1));
		}
		
		entity.delete("test");
		
		try(FetchResult<StreamingInput> fr = entity.query("byField1", IndexQuery.type())
			.field("field1")
			.isEqualTo("value1")
			.run())
		{
			assertThat(fr.getSize(), is(0));
		}
	}
	
	@Test
	public void testStoreReplace()
	{
		entity.store("test", generateList());
		
		try(FetchResult<StreamingInput> fr = entity.query("byField1", IndexQuery.type())
			.field("field1")
			.isEqualTo("value1")
			.run())
		{
			assertThat(fr.getSize(), is(1));
		}
		
		entity.store("test", generateList());
		
		try(FetchResult<StreamingInput> fr = entity.query("byField1", IndexQuery.type())
			.field("field1")
			.isEqualTo("value1")
			.run())
		{
			assertThat(fr.getSize(), is(1));
		}
	}
	
	@Test
	public void testStoreMultiple()
	{
		entity.store("test1", generateList());
		entity.store("test2", generateList());
		
		try(FetchResult<StreamingInput> fr = entity.query("byField1", IndexQuery.type())
			.field("field1")
			.isEqualTo("value1")
			.run())
		{
			assertThat(fr.getSize(), is(2));
		}
	}
}
