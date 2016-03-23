package se.l4.silo.engine.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import org.junit.Test;

import com.google.common.base.Throwables;

import se.l4.aurochs.serialization.format.BinaryInput;
import se.l4.aurochs.serialization.format.BinaryOutput;
import se.l4.aurochs.serialization.format.StreamingInput;
import se.l4.aurochs.serialization.format.StreamingInput.Token;
import se.l4.silo.FetchResult;
import se.l4.silo.IndexQuery;
import se.l4.silo.Silo;
import se.l4.silo.engine.IndexQueryEngineFactory;
import se.l4.silo.engine.LocalSilo;
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
		
		silo.stop();
		
		silo = LocalSilo.open(tmp)
			.addEntity("test")
				.asStructured()
				.defineField("field", "string")
				.add("byField", IndexQueryEngineFactory.type())
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
		
		silo.stop();
		
		DataUtils.removeRecursive(tmp);
	}
	
	private BinaryInput generateTestData()
	{
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			BinaryOutput out = new BinaryOutput(baos);
			out.writeObjectStart("");
			out.write("field", "value");
			out.writeObjectEnd("");
			out.flush();
			
			return new BinaryInput(new ByteArrayInputStream(baos.toByteArray()));
		}
		catch(IOException e)
		{
			throw Throwables.propagate(e);
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
				switch(c.getString())
				{
					case "field":
						c.next(Token.VALUE);
						if(! "value".equals(c.getString()))
						{
							throw new AssertionError("Not the same as the test data");
						}
						break;
					default:
						throw new AssertionError("Got an unknown field " + c.getString());
				}
			}
			c.next(Token.OBJECT_END);
		}
		catch(IOException e)
		{
			throw Throwables.propagate(e);
		}
			
		fr.close();
	}
}
