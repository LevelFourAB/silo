package se.l4.silo.engine.search.internal;

import java.io.IOException;
import java.util.Optional;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DoubleValues;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.IndexSearcher;

import se.l4.silo.engine.search.IndexDefinition;
import se.l4.silo.engine.search.scoring.QueryScorer;
import se.l4.silo.engine.search.scoring.ScoringEncounter;
import se.l4.silo.engine.search.scoring.ScoringProvider;

/**
 * {@link DoubleValuesSource} that calculates values using a
 * {@link ScoringProvider}.
 *
 * @author Andreas Holstenson
 *
 */
public class CustomScoreFunction<T>
	extends DoubleValuesSource
{
	private final ScoringProvider<T> provider;
	private final T data;
	private final IndexDefinition def;

	public CustomScoreFunction(
		IndexDefinition def,
		ScoringProvider<T> provider,
		T data
	)
	{
		this.def = def;

		this.provider = provider;
		this.data = data;
	}

	@Override
	public DoubleValuesSource rewrite(IndexSearcher reader)
		throws IOException
	{
		return this;
	}

	@Override
	public boolean needsScores()
	{
		return provider.needsScores();
	}

	@Override
	public DoubleValues getValues(LeafReaderContext ctx, DoubleValues scores)
		throws IOException
	{
		QueryScorer scorer = provider.createScorer(new ScoringEncounter<T>()
		{
			@Override
			public IndexDefinition getIndexDefinition()
			{
				return def;
			}

			@Override
			public LeafReaderContext getLeafReader()
			{
				return ctx;
			}

			@Override
			public T getParameters()
			{
				return data;
			}

			@Override
			public Optional<DoubleValues> getScores()
			{
				return Optional.ofNullable(scores);
			}
		});

		return new ScoringDoubleValues(ctx, scorer);
	}

	@Override
	public boolean isCacheable(LeafReaderContext ctx)
	{
		return false;
	}

	@Override
	public String toString()
	{
		return "CustomScoreFunction{provider=" + provider + ", params=" + data + "}";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((data == null) ? 0 : data.hashCode());
		result = prime * result + ((def == null) ? 0 : def.hashCode());
		result = prime * result + ((provider == null) ? 0 : provider.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CustomScoreFunction other = (CustomScoreFunction) obj;
		if (data == null) {
			if (other.data != null)
				return false;
		} else if (!data.equals(other.data))
			return false;
		if (def == null) {
			if (other.def != null)
				return false;
		} else if (!def.equals(other.def))
			return false;
		if (provider == null) {
			if (other.provider != null)
				return false;
		} else if (!provider.equals(other.provider))
			return false;
		return true;
	}

	private static class ScoringDoubleValues
		extends DoubleValues
	{
		private final QueryScorer scorer;
		private double value;

		public ScoringDoubleValues(
			LeafReaderContext context,
			QueryScorer scorer
		)
		{
			this.scorer = scorer;
		}

		@Override
		public boolean advanceExact(int doc)
			throws IOException
		{
			value = scorer.score(doc);
			return true;
		}

		@Override
		public double doubleValue()
			throws IOException
		{
			return value;
		}
	}
}
