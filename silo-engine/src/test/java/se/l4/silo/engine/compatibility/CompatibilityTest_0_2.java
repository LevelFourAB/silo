package se.l4.silo.engine.compatibility;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.util.Objects;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import se.l4.exobytes.AnnotationSerialization;
import se.l4.exobytes.Expose;
import se.l4.exobytes.Serializers;
import se.l4.silo.Entity;
import se.l4.silo.engine.EntityCodec;
import se.l4.silo.engine.EntityDefinition;
import se.l4.silo.engine.LocalSilo;
import se.l4.silo.engine.internal.DataUtils;
import se.l4.silo.engine.internal.SiloTest;

/**
 * This test ensures compatibility with the 0.2.x series when it comes to stored
 * data.
 *
 * The 0.2 compatibility file contains two entities with data:
 *
 * * `object`, long id from 1 to 1000 - DataObject serialization
 * * `binary`, int id from 1 to 100
 */
public class CompatibilityTest_0_2
	extends SiloTest
{
	private LocalSilo silo;

	@BeforeEach
	public void setup()
		throws IOException
	{
		// Copy the data file into the tmp directory
		try(InputStream in = getClass().getResourceAsStream("0_2.mv.bin");
			OutputStream out = Files.newOutputStream(tmp.resolve("storage.mv.bin")))
		{
			in.transferTo(out);
		}

		// Open the Silo instance
		silo = LocalSilo.open(tmp)
			.addEntity(
				EntityDefinition.create("object", DataObject.class)
					.withCodec(EntityCodec.serialized(Serializers.create().build(), DataObject.class))
					.withId(Long.class, DataObject::getId)
					.build()
			)
			.addEntity(
				EntityDefinition.create("binary", InMemoryBlob.class)
					.withCodec(new InMemoryBlobCodec())
					.withId(Integer.class, o -> { throw new UnsupportedOperationException(); })
					.build()
			)
			.start()
			.block();
	}

	@AfterEach
	public void close()
	{
		if(silo != null)
		{
			silo.close();
		}
	}

	@Test
	public void testObject()
	{
		Entity<Long, DataObject> entity = silo.entity("object", Long.class, DataObject.class);
		for(int i=1; i<=1000; i++)
		{
			DataObject o = entity.get((long) i).block();
			assertThat(o, is(new DataObject(i, "U" + i, i % 30, i % 2 == 0)));
		}

		assertThat(entity.get(1001l).block(), nullValue());
	}

	@Test
	public void testStoreNewObject()
	{
		Entity<Long, DataObject> entity = silo.entity("object", Long.class, DataObject.class);

		DataObject o = new DataObject(1001, "Test", 31, false);
		entity.store(o).block();

		// Test that all of the values are still available
		for(int i=1; i<=1000; i++)
		{
			DataObject l = entity.get((long) i).block();
			assertThat(l, is(new DataObject(i, "U" + i, i % 30, i % 2 == 0)));
		}

		assertThat(entity.get(1001l).block(), is(o));
	}

	@Test
	public void testBinary()
		throws IOException
	{
		Entity<Integer, InMemoryBlob> entity = silo.entity("binary", Integer.class, InMemoryBlob.class);

		for(int i=1; i<=100; i++)
		{
			InMemoryBlob o = entity.get(i).block();
			assertThat("object " + i + " is null", o, notNullValue());

			try(InputStream in = o.asStream())
			{
				DataUtils.assertBytesEquals(in, generate(i * 512));
			}
		}
	}

	public static ByteArrayInputStream generate(int size)
	{
		byte[] out = new byte[size];
		for(int i=0; i<size; i++)
		{
			out[i] = (byte) (i % 255);
		}
		return new ByteArrayInputStream(out);
	}

	@AnnotationSerialization
	public static class DataObject
	{
		@Expose
		private final long id;
		@Expose
		private final String name;
		@Expose
		private final int age;
		@Expose
		private final boolean active;

		public DataObject(
			@Expose("id") long id,
			@Expose("name") String name,
			@Expose("age") int age,
			@Expose("active") boolean active
		)
		{
			this.id = id;
			this.name = name;
			this.age = age;
			this.active = active;
		}

		public long getId()
		{
			return id;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(active, age, id, name);
		}

		@Override
		public boolean equals(Object obj)
		{
			if(this == obj)
			{
				return true;
			}
			if(obj == null)
			{
				return false;
			}
			if(getClass() != obj.getClass())
			{
				return false;
			}
			DataObject other = (DataObject) obj;
			return active == other.active
				&& age == other.age
				&& id == other.id
				&& Objects.equals(name, other.name);
		}

		@Override
		public String toString()
		{
			return "DataObject{active=" + active + ", age=" + age + ", id=" + id
				+ ", name=" + name + "}";
		}
	}

	private static class InMemoryBlob
	{
		private final byte[] data;

		public InMemoryBlob(byte[] data)
		{
			this.data = data;
		}

		public InputStream asStream()
		{
			return new ByteArrayInputStream(data);
		}
	}

	private static class InMemoryBlobCodec
		implements EntityCodec<InMemoryBlob>
	{
		@Override
		public InMemoryBlob decode(InputStream in)
			throws IOException
		{
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			in.transferTo(out);
			return new InMemoryBlob(out.toByteArray());
		}

		@Override
		public void encode(InMemoryBlob instance, OutputStream out)
			throws IOException
		{
			throw new UnsupportedEncodingException();
		}
	}
}
