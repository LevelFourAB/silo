package se.l4.silo.engine.internal;

import java.nio.file.Path;

import se.l4.silo.Silo;
import se.l4.silo.engine.LocalSilo;
import se.l4.silo.engine.QueryEngineFactory;
import se.l4.silo.engine.builder.SiloBuilder;
import se.l4.silo.engine.config.EngineConfig;
import se.l4.silo.engine.log.LogBuilder;

public class LocalSiloBuilder
	implements SiloBuilder
{
	private final LogBuilder logBuilder;
	private final Path dataPath;
	
	private EngineConfig config;

	public LocalSiloBuilder(LogBuilder logBuilder, Path dataPath)
	{
		this.logBuilder = logBuilder;
		this.dataPath = dataPath;
		
		config = new EngineConfig();
	}
	
	@Override
	public SiloBuilder addQueryEngine(String name, QueryEngineFactory factory)
	{
		return this;
	}
	
	@Override
	public Silo build()
	{
		return new LocalSilo(logBuilder, dataPath, config);
	}
}
