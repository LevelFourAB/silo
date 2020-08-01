package se.l4.silo.engine.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import org.junit.Test;

import se.l4.exobytes.streaming.StreamingFormat;
import se.l4.exobytes.streaming.StreamingInput;
import se.l4.exobytes.streaming.StreamingOutput;
import se.l4.exobytes.streaming.Token;
import se.l4.silo.FetchResult;
import se.l4.silo.Silo;
import se.l4.silo.engine.Index;
import se.l4.silo.engine.LocalSilo;
import se.l4.silo.query.IndexQuery;
import se.l4.silo.structured.StructuredEntity;

public class QueryEngineUpdateTest
{
	@Test
	public void createNewIndex()
		throws Exception
	{
		Path tmp = Files.createTempDirectory("silo");
		Silo silo = LocalSilo.open(tmp)
			.addEntity("test")
				.asStructured()
				.defineField("field", "string")
				.done()
			.build();

		StructuredEntity entity = silo.structured("test");
		entity.store("test", generateTestData());

		silo.close();

		silo = LocalSilo.open(tmp)
			.addEntity("test")
				.asStructured()
				.defineField("field", "string")
				.add("byField", Index::queryEngine)
					.addField("field")
					.done()
				.done()
			.build();

		entity = silo.structured("test");

		Thread.sleep(100);

		FetchResult<StreamingInput> fr = entity.query("byField", IndexQuery.type())
			.field("field")
			.isEqualTo("value")
			.run();

		check(fr);

		silo.close();

		DataUtils.removeRecursive(tmp);
	}

	private StreamingInput generateTestData()
	{
		try(ByteArrayOutputStream baos = new ByteArrayOutputStream();
			StreamingOutput out = StreamingFormat.LEGACY_BINARY.createOutput(baos);)
		{
			out.writeObjectStart();
			out.writeString("field");
			out.writeString("value");
			out.writeObjectEnd();
			out.flush();

			return StreamingFormat.LEGACY_BINARY.createInput(new ByteArrayInputStream(baos.toByteArray()));
		}
		catch(IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private void check(FetchResult<StreamingInput> fr)
	{
		if(fr.isEmpty())
		{
			throw new AssertionError("No data");
		}

		Iterator<StreamingInput> it = fr.iterator();
		StreamingInput c = it.next();

		try
		{
			c.next(Token.OBJECT_START);
			while(c.peek() != Token.OBJECT_END)
			{
				c.next(Token.KEY);
				switch(c.readString())
				{
					case "field":
						c.next(Token.VALUE);
						if(! "value".equals(c.readString()))
						{
							throw new AssertionError("Not the same as the test data");
						}
						break;
					default:
						throw new AssertionError("Got an unknown field " + c.readString());
				}
			}
			c.next(Token.OBJECT_END);
		}
		catch(IOException e)
		{
			throw new RuntimeException(e);
		}

		fr.close();
	}
}
