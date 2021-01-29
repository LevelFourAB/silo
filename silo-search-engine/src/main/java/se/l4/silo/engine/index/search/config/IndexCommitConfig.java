package se.l4.silo.engine.index.search.config;

import java.time.Duration;
import java.util.Objects;

import se.l4.exobytes.AnnotationSerialization;
import se.l4.exobytes.Expose;
import se.l4.exobytes.time.TemporalHints;

/**
 * Configuration related to committing the index.
 */
@AnnotationSerialization
public class IndexCommitConfig
{
	private static final int DEFAULT_MAX_UPDATES = 10000;
	private static final Duration DEFAULT_MAX_TIME = Duration.ofMinutes(1);

	@Expose
	private final int maxUpdates;

	@Expose
	@TemporalHints.Format
	private final Duration maxTime;

	public IndexCommitConfig(
		@Expose("maxUpdates") Integer maxUpdates,
		@Expose("maxTime") Duration maxTime
	)
	{
		this.maxUpdates = maxUpdates == null ? DEFAULT_MAX_UPDATES : maxUpdates;
		this.maxTime = maxTime == null ? DEFAULT_MAX_TIME : maxTime;
	}

	/**
	 * Get the maximum number of updates to keep before committing the index.
	 *
	 * @return
	 */
	public int getMaxUpdates()
	{
		return maxUpdates;
	}

	/**
	 * Get the maximum time to wait before committing the index.
	 *
	 * @return
	 */
	public Duration getMaxTime()
	{
		return maxTime;
	}

	public static Builder create()
	{
		return new Builder(DEFAULT_MAX_UPDATES, DEFAULT_MAX_TIME);
	}

	public static class Builder
	{
		private final int maxUpdates;
		private final Duration maxTime;

		private Builder(
			int maxUpdates,
			Duration maxTime
		)
		{
			this.maxUpdates = maxUpdates;
			this.maxTime = maxTime;
		}

		public Builder withMaxUpdates(int maxUpdates)
		{
			return new Builder(maxUpdates, maxTime);
		}

		public Builder withMaxTime(Duration maxTime)
		{
			Objects.requireNonNull(maxTime);

			return new Builder(maxUpdates, maxTime);
		}

		public IndexCommitConfig build()
		{
			return new IndexCommitConfig(maxUpdates, maxTime);
		}
	}
}
