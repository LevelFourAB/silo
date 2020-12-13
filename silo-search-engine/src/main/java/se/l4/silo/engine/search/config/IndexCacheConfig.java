package se.l4.silo.engine.search.config;

import org.apache.lucene.store.NRTCachingDirectory;

import se.l4.exobytes.AnnotationSerialization;
import se.l4.exobytes.Expose;

/**
 * Configuration that controls in memory caching when using NRT. This
 * maps to values in {@link NRTCachingDirectory}.
 *
 */
@AnnotationSerialization
public class IndexCacheConfig
{
	private static final double DEFAULT_MAX_MERGE_SIZE = 5;
	private static final double DEFAULT_MAX_SIZE = 60;

	@Expose
	private final boolean active;

	@Expose
	private final double maxMergeSize;

	@Expose
	private final double maxSize;

	public IndexCacheConfig(
		@Expose("active") Boolean active,
		@Expose("maxMergeSize") Double maxMergeSize,
		@Expose("maxSize") Double maxSize
	)
	{
		this.active = active == null ? true : active;
		this.maxMergeSize = maxMergeSize == null ? DEFAULT_MAX_MERGE_SIZE : maxMergeSize;
		this.maxSize = maxSize == null ? DEFAULT_MAX_SIZE : maxSize;
	}

	/**
	 * Get if caching is active.
	 *
	 * @return
	 */
	public boolean isActive()
	{
		return active;
	}

	/**
	 * Get the maximum merge size.
	 *
	 * @return
	 */
	public double getMaxMergeSize()
	{
		return maxMergeSize;
	}

	/**
	 * Get the maximum size of the cache.
	 *
	 * @return
	 */
	public double getMaxSize()
	{
		return maxSize;
	}

	public static IndexCacheConfig none()
	{
		return new IndexCacheConfig(false, null, null);
	}

	public static Builder create()
	{
		return new Builder(true, DEFAULT_MAX_MERGE_SIZE, DEFAULT_MAX_SIZE);
	}

	public static class Builder
	{
		private final boolean active;
		private final double maxMergeSize;
		private final double maxSize;

		public Builder(
			boolean active,
			double maxMergeSize,
			double maxSize
		)
		{
			this.active = active;
			this.maxMergeSize = maxMergeSize;
			this.maxSize = maxSize;
		}

		public Builder withActive(boolean active)
		{
			return new Builder(active, maxMergeSize, maxSize);
		}

		public Builder withMaxMergeSize(double maxMergeSize)
		{
			return new Builder(active, maxMergeSize, maxSize);
		}

		public Builder withMaxSize(double maxSize)
		{
			return new Builder(active, maxMergeSize, maxSize);
		}

		public IndexCacheConfig build()
		{
			return new IndexCacheConfig(active, maxMergeSize, maxSize);
		}
	}
}
