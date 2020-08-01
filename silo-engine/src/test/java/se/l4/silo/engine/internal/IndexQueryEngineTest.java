package se.l4.silo.engine.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import se.l4.exobytes.streaming.StreamingFormat;
import se.l4.exobytes.streaming.StreamingInput;
import se.l4.exobytes.streaming.StreamingOutput;
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
				.defineField("field3.test")
					.setType("string")
					.done()
				.add("byField1", Index::queryEngine)
					.addField("field1")
					.done()
				.add("byField2", Index::queryEngine)
					.addField("field2")
					.done()
				.add("byField3", Index::queryEngine)
					.addField("field3.test")
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

	private StreamingInput generateList()
	{
		try(ByteArrayOutputStream baos = new ByteArrayOutputStream();
			StreamingOutput out = StreamingFormat.LEGACY_BINARY.createOutput(baos))
		{
			out.writeObjectStart();
				out.writeString("field1");
				out.writeListStart();
					out.writeString("value1");
					out.writeString("value2");
				out.writeListEnd();

				out.writeString("field2");
				out.writeBoolean(false);
			out.writeObjectEnd();
			out.flush();

			return StreamingFormat.LEGACY_BINARY.createInput(new ByteArrayInputStream(baos.toByteArray()));
		}
		catch(IOException e)
		{
			throw new RuntimeException(e);
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

		assertThat(fr.getSize(), is(1l));
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
			assertThat(fr.getSize(), is(1l));
		}

		entity.delete("test");

		try(FetchResult<StreamingInput> fr = entity.query("byField1", IndexQuery.type())
			.field("field1")
			.isEqualTo("value1")
			.run())
		{
			assertThat(fr.getSize(), is(0l));
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
			assertThat(fr.getSize(), is(1l));
		}

		entity.store("test", generateList());

		try(FetchResult<StreamingInput> fr = entity.query("byField1", IndexQuery.type())
			.field("field1")
			.isEqualTo("value1")
			.run())
		{
			assertThat(fr.getSize(), is(1l));
		}
	}

	@Test
	public void testStoreMultiple1()
	{
		entity.store("test1", generateList());
		entity.store("test2", generateList());

		try(FetchResult<StreamingInput> fr = entity.query("byField1", IndexQuery.type())
			.field("field1")
			.isEqualTo("value1")
			.run())
		{
			assertThat(fr.getSize(), is(2l));
		}
	}

	@Test
	public void testStoreMultiple2()
	{
		entity.store("test1", generateList());
		entity.store("test2", generateList());

		try(FetchResult<StreamingInput> fr = entity.query("byField1", IndexQuery.type())
			.field("field1")
			.isEqualTo("value2")
			.run())
		{
			assertThat(fr.getSize(), is(2l));
		}
	}

	@Test
	public void testStoreDeleteStore()
	{
		entity.store("test", generateList());

		try(FetchResult<StreamingInput> fr = entity.query("byField1", IndexQuery.type())
			.field("field1")
			.isEqualTo("value1")
			.run())
		{
			assertThat(fr.getSize(), is(1l));
		}

		entity.delete("test");

		try(FetchResult<StreamingInput> fr = entity.query("byField1", IndexQuery.type())
			.field("field1")
			.isEqualTo("value1")
			.run())
		{
			assertThat(fr.getSize(), is(0l));
		}

		entity.store("test", generateList());

		try(FetchResult<StreamingInput> fr = entity.query("byField1", IndexQuery.type())
			.field("field1")
			.isEqualTo("value1")
			.run())
		{
			assertThat(fr.getSize(), is(1l));
		}
	}

	@Test
	public void testStoreWithPath()
	{
		StreamingInput in;
		try(ByteArrayOutputStream baos = new ByteArrayOutputStream();
			StreamingOutput out = StreamingFormat.LEGACY_BINARY.createOutput(baos))
		{
			out.writeObjectStart();
				out.writeString("field3");
				out.writeObjectStart();
					out.writeString("test");
					out.writeString("hello");
				out.writeObjectEnd();
			out.writeObjectEnd();
			out.flush();

			in = StreamingFormat.LEGACY_BINARY.createInput(new ByteArrayInputStream(baos.toByteArray()));
		}
		catch(IOException e)
		{
			throw new RuntimeException(e);
		}

		entity.store("test", in);

		try(FetchResult<StreamingInput> fr = entity.query("byField3", IndexQuery.type())
			.field("field3.test")
			.isEqualTo("hello")
			.run())
		{
			assertThat(fr.getSize(), is(1l));
		}

		entity.delete("test");

		try(FetchResult<StreamingInput> fr = entity.query("byField3", IndexQuery.type())
			.field("field3.test")
			.isEqualTo("value1")
			.run())
		{
			assertThat(fr.getSize(), is(0l));
		}
	}
}
