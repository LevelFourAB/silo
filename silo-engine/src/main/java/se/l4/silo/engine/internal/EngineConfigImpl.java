package se.l4.silo.engine.internal;

import se.l4.silo.engine.EngineConfig;

/**
 * Implementation of {@link EngineConfig}.
 */
public class EngineConfigImpl
	implements EngineConfig
{
	private final int cacheSizeInMiB;
	private final int cacheConcurrency;
	private final int autoCompactFillRate;
	private final int autoCommitBufferSizeInKiB;

	public EngineConfigImpl(
		int cacheSizeInMiB,
		int cacheConcurrency,
		int autoCompactFillRate,
		int autoCommitBufferSizeInKiB
	)
	{
		this.cacheSizeInMiB = cacheSizeInMiB;
		this.cacheConcurrency = cacheConcurrency;
		this.autoCompactFillRate = autoCompactFillRate;
		this.autoCommitBufferSizeInKiB = autoCommitBufferSizeInKiB;
	}

	@Override
	public int getCacheSizeInMiB()
	{
		return cacheSizeInMiB;
	}

	@Override
	public int getCacheConcurrency()
	{
		return cacheConcurrency;
	}

	@Override
	public int getAutoCompactFillRate()
	{
		return autoCompactFillRate;
	}

	@Override
	public int getAutoCommitBufferSizeInKiB()
	{
		return autoCommitBufferSizeInKiB;
	}

	public static Builder create()
	{
		return new BuilderImpl(
			EngineConfig.CACHE_SIZE_IN_MIB,
			EngineConfig.CACHE_CONCURRENCY,
			EngineConfig.AUTO_COMPACT_FILL_RATE,
			EngineConfig.AUTO_COMMIT_BUFFER_SIZE
		);
	}

	private static class BuilderImpl
		implements Builder
	{
		private final int cacheSizeInMiB;
		private final int cacheConcurrency;
		private final int autoCompactFillRate;
		private final int autoCommitBufferSizeInKiB;

		public BuilderImpl(
			int cacheSizeInMiB,
			int cacheConcurrency,
			int autoCompactFillRate,
			int autoCommitBufferSizeInKiB
		)
		{
			this.cacheSizeInMiB = cacheSizeInMiB;
			this.cacheConcurrency = cacheConcurrency;
			this.autoCompactFillRate = autoCompactFillRate;
			this.autoCommitBufferSizeInKiB = autoCommitBufferSizeInKiB;
		}

		@Override
		public Builder withCacheSizeInMiB(int sizeInMiB)
		{
			if(sizeInMiB < 16)
			{
				throw new IllegalArgumentException("cacheSizeInMiB can't be less than 16 MiB");
			}

			return new BuilderImpl(
				sizeInMiB,
				cacheConcurrency,
				autoCompactFillRate,
				autoCommitBufferSizeInKiB
			);
		}

		@Override
		public Builder withCacheConcurrency(int concurrency)
		{
			if(concurrency < 1)
			{
				throw new IllegalArgumentException("cacheConcurrency can't be less than 1");
			}

			return new BuilderImpl(
				cacheSizeInMiB,
				concurrency,
				autoCompactFillRate,
				autoCommitBufferSizeInKiB
			);
		}

		@Override
		public Builder withAutoCompactFillRate(int percentage)
		{
			if(percentage < 0)
			{
				throw new IllegalArgumentException("autoCompactFillRate can't be less than 0");
			}

			if(percentage > 95)
			{
				throw new IllegalArgumentException("autoCompactFillRate can't be more than 95");
			}

			return new BuilderImpl(
				cacheSizeInMiB,
				cacheConcurrency,
				percentage,
				autoCommitBufferSizeInKiB
			);
		}

		@Override
		public Builder withAutoCommitBufferSizeInKiB(int sizeInKiB)
		{
			if(sizeInKiB < 0)
			{
				throw new IllegalArgumentException("autoCommitBufferSizeInKiB can't be less than 0");
			}

			return new BuilderImpl(
				cacheSizeInMiB,
				cacheConcurrency,
				autoCompactFillRate,
				sizeInKiB
			);
		}

		@Override
		public EngineConfig build()
		{
			return new EngineConfigImpl(
				cacheSizeInMiB,
				cacheConcurrency,
				autoCompactFillRate,
				autoCommitBufferSizeInKiB
			);
		}
	}
}
