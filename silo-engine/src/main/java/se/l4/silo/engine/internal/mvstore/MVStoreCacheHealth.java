package se.l4.silo.engine.internal.mvstore;

import org.h2.mvstore.MVStore;
import org.h2.mvstore.Page;
import org.h2.mvstore.cache.CacheLongKeyLIRS;

import se.l4.vibe.mapping.KeyValueMappable;
import se.l4.vibe.mapping.KeyValueReceiver;
import se.l4.vibe.probes.AbstractSampledProbe;
import se.l4.vibe.probes.SampledProbe;

/**
 * Cache health information extracted from a {@link MVStore}.
 * 
 * @author Andreas Holstenson
 *
 */
public class MVStoreCacheHealth
	implements KeyValueMappable
{
	private final long hits;
	private final long misses;
	private final long usedMemory;
	private final long maxMemory;

	public MVStoreCacheHealth(long hits, long misses, long usedMemory, long maxMemory)
	{
		this.hits = hits;
		this.misses = misses;
		this.usedMemory = usedMemory;
		this.maxMemory = maxMemory;
	}

	@Override
	public void mapToKeyValues(KeyValueReceiver receiver)
	{
		receiver.add("hits", hits);
		receiver.add("misses", misses);
		receiver.add("memoryUsed", usedMemory);
		receiver.add("memoryMax", maxMemory);
		receiver.add("memoryUsageAsFraction", usedMemory / (double) maxMemory);
	}
	
	public static SampledProbe<MVStoreCacheHealth> createProbe(MVStore store)
	{
		return new AbstractSampledProbe<MVStoreCacheHealth>()
		{
			private long hits;
			private long misses;
			
			@Override
			public MVStoreCacheHealth peek()
			{
				return sample0();
			}
			
			@Override
			protected MVStoreCacheHealth sample0()
			{
				CacheLongKeyLIRS<Page> cache = store.getCache();
				long hits = cache.getHits();
				long misses = cache.getMisses();
				
				long usedMemory = cache.getUsedMemory();
				long maxMemory = cache.getMaxMemory();
				
				MVStoreCacheHealth result = new MVStoreCacheHealth(
					hits - this.hits,
					misses - this.misses,
					usedMemory,
					maxMemory
				);
				
				this.hits = hits;
				this.misses = misses;
				
				return result;
			}
		};
	}
}
