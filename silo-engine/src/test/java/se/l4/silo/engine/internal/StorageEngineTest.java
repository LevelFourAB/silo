package se.l4.silo.engine.internal;

import java.io.File;

import se.l4.silo.engine.config.EngineConfig;
import se.l4.silo.engine.config.EntityConfig;
import se.l4.silo.engine.log.DirectApplyLog;

public class StorageEngineTest
{
	public StorageEngineTest()
	{
		EngineConfig config = new EngineConfig()
			.addEntity("test", new EntityConfig());
		
		StorageEngine engine = new StorageEngine(DirectApplyLog.builder(), new File("data").toPath(), config);
	}
	
	public static void main(String[] args)
	{
		new StorageEngineTest();
	}
}
