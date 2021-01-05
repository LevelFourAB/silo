package se.l4.silo.engine.index.search.config;

import se.l4.exobytes.AnnotationSerialization;
import se.l4.exobytes.Expose;

@AnnotationSerialization
public class IndexCommitConfig
{
	@Expose
	private int maxUpdates;

	@Expose
	private int maxTime;

	public IndexCommitConfig()
	{
		maxUpdates = 1000;
		maxTime = 60;
	}

	public int getMaxUpdates()
	{
		return maxUpdates;
	}

	public int getMaxTime()
	{
		return maxTime;
	}
}
