package se.l4.silo.engine.index.search.config;

import se.l4.exobytes.AnnotationSerialization;
import se.l4.exobytes.Expose;

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
 */
@AnnotationSerialization
public class IndexFreshnessConfig
{
	@Expose
	private double maxStale;

	@Expose
	private double minStale;

	public IndexFreshnessConfig()
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
	 * Get the minimum amount of seconds to wait until changes are made
	 * available.
	 *
	 * @return
	 */
	public double getMinStale()
	{
		return minStale;
	}
}
