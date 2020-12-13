package se.l4.silo.engine;

/**
 * Shared configuration that can be specified for a {@link LocalSilo} instance.
 */
public class EngineConfig
{
	private final int cacheSizeInMb;

	public EngineConfig()
	{
		this(128);
	}

	public EngineConfig(
		int cacheSizeInMb
	)
	{
		this.cacheSizeInMb = cacheSizeInMb;
	}

	public int getCacheSizeInMb()
	{
		return cacheSizeInMb;
	}

	public EngineConfig setCacheSizeInMb(int cacheSizeInMb)
	{
		return new EngineConfig(cacheSizeInMb);
	}
}
