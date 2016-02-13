package se.l4.silo.engine.internal;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import se.l4.aurochs.core.io.Bytes;
import se.l4.silo.FetchResult;
import se.l4.silo.Silo;
import se.l4.silo.Transaction;
import se.l4.silo.binary.BinaryEntity;
import se.l4.silo.binary.BinaryEntry;
import se.l4.silo.engine.LocalSilo;
import se.l4.silo.engine.config.EngineConfig;
import se.l4.silo.engine.config.EntityConfig;

public class BinaryEntityTest
{
	private Silo silo;
	private Path tmp;
	private BinaryEntity entity;

	@Before
	public void before()
		throws IOException
	{
		EngineConfig config = new EngineConfig()
			.addEntity("test", new EntityConfig());
		
		tmp = Files.createTempDirectory("silo");
		silo = LocalSilo.open(tmp, config);
		
		entity = silo.binary("test");
	}
	
	@After
	public void after() throws Exception
	{
		silo.stop();
		Files.walkFileTree(tmp, new SimpleFileVisitor<Path>()
		{
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
				throws IOException
			{
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc)
				throws IOException
			{
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}

		});
	}
	
	private void check(FetchResult<BinaryEntry> fr, Bytes data)
	{
		BinaryEntry be = fr.iterator().next();
		DataUtils.assertBytesEquals(data, be.getData());
	}
	
	@Test
	public void testStoreNoTransaction()
	{
		Bytes data = DataUtils.generate(615);
		entity.store("test", data);
		
		check(entity.get("test"), data);
	}
	
	
	@Test
	public void testStoreInTransaction()
	{
		Bytes data = DataUtils.generate(615);
		Transaction tx = silo.newTransaction();
		
		entity.store("test", data);
		
		assertEquals(entity.get("test").isEmpty(), true);
			
		tx.commit();
		
		check(entity.get("test"), data);
	}
	
	@Test
	public void testStoreDeleteNoTransaction()
	{
		Bytes data = DataUtils.generate(2056);
		entity.store("test", data);
		check(entity.get("test"), data);
		entity.delete("test");
		assertEquals(entity.get("test").isEmpty(), true);
	}
	
	@Test
	public void testStoreDeleteInTransaction()
	{
		Bytes data = DataUtils.generate(2056);
		
		Transaction tx = silo.newTransaction();
		entity.store("test", data);
		tx.commit();
		
		check(entity.get("test"), data);
		
		tx = silo.newTransaction();
		entity.delete("test");
		tx.commit();
		
		assertEquals(entity.get("test").isEmpty(), true);
	}
}
