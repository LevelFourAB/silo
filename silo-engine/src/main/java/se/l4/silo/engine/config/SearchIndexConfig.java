package se.l4.silo.engine.config;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.apache.lucene.store.NRTCachingDirectory;

import se.l4.commons.serialization.Expose;
import se.l4.commons.serialization.ReflectionSerializer;
import se.l4.commons.serialization.Use;

public class SearchIndexConfig
	extends QueryEngineConfig
{
	private IndexReloadConfig reload;
	
	private List<FieldConfig> fields;
	
	public SearchIndexConfig()
	{
		super("silo:search-index");
		
		reload = new IndexReloadConfig();
		fields = new ArrayList<>();
	}
	
	public IndexReloadConfig getReload()
	{
		return reload;
	}
	
	public List<FieldConfig> getFields()
	{
		return fields;
	}
	
	public void addField(FieldConfig fc)
	{
		fields.add(fc);
	}

	public static class FieldConfig
	{
		private final String name;
		
		public FieldConfig(String name)
		{
			this.name = name;
		}
		
		public String getName()
		{
			return name;
		}
	}
	
	public static class IndexReloadConfig
	{
		@Expose
		@NotNull
		private Cache cache;
		
		@Expose
		@NotNull
		private Freshness freshness;
		
		public IndexReloadConfig()
		{
			cache = new Cache();
			freshness = new Freshness();
		}
		
		/**
		 * Get information about caching.
		 * 
		 * @return
		 */
		public Cache getCache()
		{
			return cache;
		}
		
		/**
		 * Control how caching should be used.
		 * 
		 * @param cache
		 * @return
		 */
		public IndexReloadConfig setCache(Cache cache)
		{
			if(cache == null)
			{
				throw new IllegalArgumentException("Cache can not be null, to disable caching use Cache.setActive(false)");
			}
			
			this.cache = cache;
			
			return this;
		}
		
		/**
		 * Get configuration for the freshness of the index.
		 * 
		 * @return
		 */
		public Freshness getFreshness()
		{
			return freshness;
		}
		
		/**
		 * Set the freshness of the index.
		 * 
		 * @param freshness
		 * @return 
		 */
		public IndexReloadConfig setFreshness(Freshness freshness)
		{
			if(freshness == null)
			{
				throw new IllegalArgumentException("Freshness can not be null");
			}
			
			this.freshness = freshness;
			
			return this;
		}
	}
	
	/**
	 * Configuration that controls in memory caching when using NRT. This
	 * maps to values in {@link NRTCachingDirectory}. To disable caching
	 * set 
	 * 
	 * @author Andreas Holstenson
	 *
	 */
	@Use(ReflectionSerializer.class)
	public static class Cache
	{
		@Expose
		private boolean active;
		
		@Expose
		private double maxMergeSize;
		
		@Expose
		private double maxSize;
		
		public Cache()
		{
			active = true;
			maxMergeSize = 5;
			maxSize = 60;
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
		 * Set if caching should be active.
		 * 
		 * @param active
		 * @return
		 */
		public Cache setActive(boolean active)
		{
			this.active = active;
			
			return this;
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
		 * Set the maximum size in MBs of cached segments
		 * 
		 * @param maxMergeSize
		 * @return
		 */
		public Cache setMaxMergeSize(double maxMergeSize)
		{
			this.maxMergeSize = maxMergeSize;
			
			return this;
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
		
		/**
		 * Set the maximum size of the cache in MBs.
		 * 
		 * @param maxSize
		 */
		public void setMaxSize(double maxSize)
		{
			this.maxSize = maxSize;
		}
	}
	
	/**
	 * Configuration to control the freshness of the index, this will control
	 * how often the index is refreshed so that it is possible to perform
	 * searches on newly added content.
	 * 
	 * <p>
	 * Default values are to wait a maximum of 1 second and a minimum time
	 * of 0.1 seconds. This means that results that are 1 second old will
	 * always be available.
	 * 
	 * @author Andreas Holstenson
	 *
	 */
	@Use(ReflectionSerializer.class)
	public static class Freshness
	{
		@Expose
		private double maxStale;
		
		@Expose
		private double minStale;
		
		public Freshness()
		{
			maxStale = 1.0;
			minStale = 0.1;
		}
		
		/**
		 * Get the maximum amount of seconds to wait until changes are made
		 * available.
		 * 
		 * @return
		 */
		public double getMaxStale()
		{
			return maxStale;
		}
		
		/**
		 * Set the maximum amount of seconds to wait until changes are made
		 * available.
		 * 
		 * @param maxStale
		 * @return
		 */
		public Freshness setMaxStale(double maxStale)
		{
			this.maxStale = maxStale;
			
			return this;
		}
		
		/**
		 * Get the minimum amount of seconds to wait until changes are made
		 * available.
		 * 
		 * @return
		 */
		public double getMinStale()
		{
			return minStale;
		}
		
		/**
		 * Set the minimum amount of seconds to wait until a changes are made
		 * available. This is useful as making changes available are somewhat
		 * expensive.
		 * 
		 * @param minStale
		 * @return
		 */
		public Freshness setMinStale(double minStale)
		{
			this.minStale = minStale;
			
			return this;
		}
	}
}
