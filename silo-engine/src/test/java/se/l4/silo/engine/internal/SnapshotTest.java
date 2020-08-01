package se.l4.silo.engine.internal;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.io.ByteStreams;

import org.junit.Assert;
import org.junit.Test;

import se.l4.silo.FetchResult;
import se.l4.silo.binary.BinaryEntity;
import se.l4.silo.binary.BinaryEntry;
import se.l4.silo.engine.FileSnapshot;
import se.l4.silo.engine.Index;
import se.l4.silo.engine.LocalSilo;
import se.l4.silo.engine.Snapshot;
import se.l4.silo.structured.ObjectEntity;
import se.l4.ylem.io.Bytes;

public class SnapshotTest
{
	@Test
	public void testUpdateExistingNoIndexes()
		throws Exception
	{
		Path tmp = Files.createTempDirectory("silo");
		Path tmpFile = Files.createTempFile("silobackup", ".bin");
		try
		{
			LocalSilo silo = LocalSilo.open(tmp)
				.addEntity("test").asBinary().done()
				.build();

			BinaryEntity entity = silo.binary("test");
			entity.store("e1", DataUtils.generate(1024));
			entity.store("e2", DataUtils.generate(40212));

			// Store the snapshot
			try(Snapshot snapshot = silo.createSnapshot(); OutputStream out = new FileOutputStream(tmpFile.toString()))
			{
				try(InputStream in = snapshot.asStream())
				{
					ByteStreams.copy(in, out);
				}
			}

			// Restore the snapshot
			silo.installSnapshot(new FileSnapshot(tmpFile));

			check(entity.get("e2"), DataUtils.generate(40212));

			// Stop Silo
			silo.close();
		}
		finally
		{
			DataUtils.removeRecursive(tmp);
			Files.delete(tmpFile);
		}
	}

	@Test
	public void testUpdateExistingReopenNoIndexes()
		throws Exception
	{
		Path tmp = Files.createTempDirectory("silo");
		Path tmpFile = Files.createTempFile("silobackup", ".bin");
		try
		{
			LocalSilo silo = LocalSilo.open(tmp)
				.addEntity("test").asBinary().done()
				.build();

			BinaryEntity entity = silo.binary("test");
			entity.store("e1", DataUtils.generate(1024));
			entity.store("e2", DataUtils.generate(40212));

			// Store the snapshot
			try(Snapshot snapshot = silo.createSnapshot(); OutputStream out = new FileOutputStream(tmpFile.toString()))
			{
				try(InputStream in = snapshot.asStream())
				{
					ByteStreams.copy(in, out);
				}
			}

			// Restore the snapshot
			silo.installSnapshot(new FileSnapshot(tmpFile));

			// Stop the instance
			silo.close();

			// Restart
			silo = LocalSilo.open(tmp)
				.addEntity("test").asBinary().done()
				.build();

			entity = silo.binary("test");

			check(entity.get("e2"), DataUtils.generate(40212));

			// Stop Silo
			silo.close();
		}
		finally
		{
			DataUtils.removeRecursive(tmp);
			Files.delete(tmpFile);
		}
	}

	@Test
	public void testCreateNewInstanceNoIndexes()
		throws Exception
	{
		Path tmp = Files.createTempDirectory("silo");
		Path tmp2 = Files.createTempDirectory("silo");
		Path tmpFile = Files.createTempFile("silobackup", ".bin");
		try
		{
			LocalSilo silo = LocalSilo.open(tmp)
				.addEntity("test").asBinary().done()
				.build();

			BinaryEntity entity = silo.binary("test");
			entity.store("e1", DataUtils.generate(1024));
			entity.store("e2", DataUtils.generate(40212));

			// Store the snapshot
			try(Snapshot snapshot = silo.createSnapshot(); OutputStream out = new FileOutputStream(tmpFile.toString()))
			{
				try(InputStream in = snapshot.asStream())
				{
					ByteStreams.copy(in, out);
				}
			}

			// Stop current Silo
			silo.close();

			// Create a new instance in another directory
			silo = LocalSilo.open(tmp2)
				.addEntity("test").asBinary().done()
				.build();

			// Restore the snapshot
			silo.installSnapshot(new FileSnapshot(tmpFile));

			// Check that the new instance has our data
			entity = silo.binary("test");
			check(entity.get("e2"), DataUtils.generate(40212));

			// Stop the instance
			silo.close();
		}
		finally
		{
			DataUtils.removeRecursive(tmp);
			DataUtils.removeRecursive(tmp2);
			Files.delete(tmpFile);
		}
	}

	@Test
	public void testUpdateExistingWithIndex()
		throws Exception
	{
		Path tmp = Files.createTempDirectory("silo");
		Path tmpFile = Files.createTempFile("silobackup", ".bin");
		try
		{
			LocalSilo silo = LocalSilo.open(tmp)
				.addEntity("test")
					.asStructured()
					.defineField("name", "string")
					.add("byName", Index::queryEngine)
						.addField("name")
						.done()
					.done()
				.build();

			ObjectEntity<TestUserData> entity = silo.structured("test")
				.asObject(TestUserData.class, TestUserData::getId);
			entity.store(new TestUserData(1, "john", 22, true));
			entity.store(new TestUserData(2, "jane", 22, false));

			// Store the snapshot
			try(Snapshot snapshot = silo.createSnapshot(); OutputStream out = new FileOutputStream(tmpFile.toString()))
			{
				try(InputStream in = snapshot.asStream())
				{
					ByteStreams.copy(in, out);
				}
			}

			// Restore the snapshot
			silo.installSnapshot(new FileSnapshot(tmpFile));

			Assert.assertEquals(new TestUserData(2, "jane", 22, false), entity.get(2).get());

			// Stop Silo
			silo.close();
		}
		finally
		{
			DataUtils.removeRecursive(tmp);
			Files.delete(tmpFile);
		}
	}

	@Test
	public void testUpdateExistingReloadWithIndex()
		throws Exception
	{
		Path tmp = Files.createTempDirectory("silo");
		Path tmpFile = Files.createTempFile("silobackup", ".bin");
		try
		{
			LocalSilo silo = LocalSilo.open(tmp)
				.addEntity("test")
					.asStructured()
					.defineField("name", "string")
					.add("byName", Index::queryEngine)
						.addField("name")
						.done()
					.done()
				.build();

			ObjectEntity<TestUserData> entity = silo.structured("test")
				.asObject(TestUserData.class, TestUserData::getId);
			entity.store(new TestUserData(1, "john", 22, true));
			entity.store(new TestUserData(2, "jane", 22, false));

			// Store the snapshot
			try(Snapshot snapshot = silo.createSnapshot(); OutputStream out = new FileOutputStream(tmpFile.toString()))
			{
				try(InputStream in = snapshot.asStream())
				{
					ByteStreams.copy(in, out);
				}
			}

			// Restore the snapshot
			silo.installSnapshot(new FileSnapshot(tmpFile));

			// Stop Silo
			silo.close();

			silo = LocalSilo.open(tmp)
				.addEntity("test")
					.asStructured()
					.defineField("name", "string")
					.add("byName", Index::queryEngine)
						.addField("name")
						.done()
					.done()
				.build();

			entity = silo.structured("test")
				.asObject(TestUserData.class, TestUserData::getId);

			Assert.assertEquals(new TestUserData(2, "jane", 22, false), entity.get(2).get());

			// Stop Silo
			silo.close();
		}
		finally
		{
			DataUtils.removeRecursive(tmp);
			Files.delete(tmpFile);
		}
	}

	@Test
	public void testCreateNewInstancedWithIndex()
		throws Exception
	{
		Path tmp = Files.createTempDirectory("silo");
		Path tmp2 = Files.createTempDirectory("silo");
		Path tmpFile = Files.createTempFile("silobackup", ".bin");
		try
		{
			LocalSilo silo = LocalSilo.open(tmp)
				.addEntity("test")
					.asStructured()
					.defineField("name", "string")
					.add("byName", Index::queryEngine)
						.addField("name")
						.done()
					.done()
				.build();

			ObjectEntity<TestUserData> entity = silo.structured("test")
				.asObject(TestUserData.class, TestUserData::getId);
			entity.store(new TestUserData(1, "john", 22, true));
			entity.store(new TestUserData(2, "jane", 22, false));

			// Store the snapshot
			try(Snapshot snapshot = silo.createSnapshot(); OutputStream out = new FileOutputStream(tmpFile.toString()))
			{
				try(InputStream in = snapshot.asStream())
				{
					ByteStreams.copy(in, out);
				}
			}

			// Stop Silo
			silo.close();

			silo = LocalSilo.open(tmp2)
				.addEntity("test")
					.asStructured()
					.defineField("name", "string")
					.add("byName", Index::queryEngine)
						.addField("name")
						.done()
					.done()
				.build();

			// Restore the snapshot
			silo.installSnapshot(new FileSnapshot(tmpFile));

			entity = silo.structured("test")
				.asObject(TestUserData.class, TestUserData::getId);

			Assert.assertEquals(new TestUserData(2, "jane", 22, false), entity.get(2).get());

			// Stop Silo
			silo.close();
		}
		finally
		{
			DataUtils.removeRecursive(tmp);
			DataUtils.removeRecursive(tmp2);
			Files.delete(tmpFile);
		}
	}

	private void check(FetchResult<BinaryEntry> fr, Bytes data)
	{
		BinaryEntry be = fr.iterator().next();
		DataUtils.assertBytesEquals(data, be.getData());
	}
}
