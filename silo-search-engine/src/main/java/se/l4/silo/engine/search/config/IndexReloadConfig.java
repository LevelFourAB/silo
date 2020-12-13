package se.l4.silo.engine.search.config;

import se.l4.exobytes.AnnotationSerialization;
import se.l4.exobytes.Expose;

@AnnotationSerialization
public class IndexReloadConfig
{
	@Expose
	private final IndexCacheConfig cache;

	@Expose
	private final IndexFreshnessConfig freshness;

	public IndexReloadConfig(
		@Expose("cache") IndexCacheConfig cache,
		@Expose("freshness") IndexFreshnessConfig freshness
	)
	{
		this.cache = cache;
		this.freshness = freshness;
	}

	/**
	 * Get configuration for how the cache of this index is managed.
	 *
	 * @return
	 */
	public IndexCacheConfig getCache()
	{
		return cache;
	}

	public IndexFreshnessConfig getFreshness()
	{
		return freshness;
	}
}
