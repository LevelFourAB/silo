package se.l4.silo.engine.search.internal;

import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.queries.CustomScoreProvider;
import org.apache.lucene.queries.CustomScoreQuery;
import org.apache.lucene.search.Query;

import se.l4.silo.engine.search.IndexDefinition;
import se.l4.silo.engine.search.scoring.QueryScorer;
import se.l4.silo.engine.search.scoring.ScoringEncounter;
import se.l4.silo.engine.search.scoring.ScoringProvider;

/**
 * Adapter for {@link CustomScoreQuery} and {@link ScoringProvider}.
 *
 * @author Andreas Holstenson
 *
 */
public class CustomScoreAdapter<T>
	extends CustomScoreQuery
{
	private final ScoringProvider<T> provider;
	private final T data;
	private final IndexDefinition def;

	public CustomScoreAdapter(IndexDefinition def, Query subQuery, ScoringProvider<T> provider, T data)
	{
		super(subQuery);
		this.def = def;

		this.provider = provider;
		this.data = data;
	}

	@Override
	protected CustomScoreProvider getCustomScoreProvider(LeafReaderContext context)
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
				return context;
			}

			@Override
			public T getParameters()
			{
				return data;
			}
		});

		return new Scorer(context, scorer);
	}

	private static class Scorer
		extends CustomScoreProvider
	{
		private final QueryScorer actualScorer;

		public Scorer(LeafReaderContext context, QueryScorer actualScorer)
		{
			super(context);
			this.actualScorer = actualScorer;
		}

		@Override
		public float customScore(int doc, float subQueryScore, float valSrcScore)
			throws IOException
		{
			return actualScorer.score(doc,  subQueryScore);
		}
	}
}
