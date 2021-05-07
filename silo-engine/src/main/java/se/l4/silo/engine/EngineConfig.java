package se.l4.silo.engine;

import se.l4.silo.engine.internal.EngineConfigImpl;

/**
 * Configuration for an instance of {@link LocalSilo}.
 */
public interface EngineConfig
{
	/**
	 * Default {@link #getCacheSizeInMiB() cache size in MiB}.
	 */
	static int CACHE_SIZE_IN_MIB = 128;

	/**
	 * Default {@link #getCacheConcurrency() cache concurrency}.
	 */
	static int CACHE_CONCURRENCY = 16;

	/**
	 * Default {@link #getAutoCompactFillRate() fill rate before auto-compaction}.
	 */
	static int AUTO_COMPACT_FILL_RATE = 40;

	/**
	 * Default {@link #getAutoCommitBufferSizeInKiB() auto commit buffer size}.
	 */
	static int AUTO_COMMIT_BUFFER_SIZE = 1024;

	/**
	 * Get the cache size to use.
	 *
	 * @return
	 *   size in MiB
	 */
	int getCacheSizeInMiB();

	/**
	 * Get the cache concurrency to use.
	 *
	 * @return
	 */
	int getCacheConcurrency();

	/**
	 * Get the fill rate to target. If the active data falls below this
	 * percentage the storage will try to compact the storage file.
	 *
	 * @return
	 */
	int getAutoCompactFillRate();

	/**
	 * Get the amount of KiB that can be written before changes will be
	 * automatically bew written to disk.
	 *
	 * @return
	 */
	int getAutoCommitBufferSizeInKiB();

	/**
	 * Start building an instance of {@link EngineConfig}.
	 *
	 * @return
	 */
	static Builder create()
	{
		return EngineConfigImpl.create();
	}

	/**
	 * Builder for creating instances of {@link EngineConfig}.
	 */
	interface Builder
	{
		/**
		 * Set the cache size in MiB. Defaults to 128 MiB.
		 *
		 * @param sizeInMiB
		 * @return
		 */
		Builder withCacheSizeInMiB(int sizeInMiB);

		/**
		 * Set the cache concurrency. Defaults to 16.
		 *
		 * @param concurrency
		 * @return
		 */
		Builder withCacheConcurrency(int concurrency);

		/**
		 * Set the fill rate to target, when active data falls below this
		 * percentage the data file will be automatically compacted.
		 *
		 * @param percentage
		 * @return
		 */
		Builder withAutoCompactFillRate(int percentage);

		/**
		 * Set the the number of KiB that can be written before an automatic
		 * commit to disk happens.
		 *
		 * @param sizeInKiB
		 * @return
		 */
		Builder withAutoCommitBufferSizeInKiB(int sizeInKiB);

		/**
		 * Build the instance.
		 *
		 * @return
		 */
		EngineConfig build();
	}
}
