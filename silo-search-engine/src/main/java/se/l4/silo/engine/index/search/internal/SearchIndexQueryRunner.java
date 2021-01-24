package se.l4.silo.engine.index.search.internal;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.DocValuesFieldExistsQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.util.BytesRef;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.silo.FetchResult;
import se.l4.silo.StorageException;
import se.l4.silo.engine.TransactionValue;
import se.l4.silo.engine.index.IndexQueryEncounter;
import se.l4.silo.engine.index.IndexQueryRunner;
import se.l4.silo.engine.index.search.SearchFieldDefinition;
import se.l4.silo.engine.index.search.SearchIndexEncounter;
import se.l4.silo.engine.index.search.locales.LocaleSupport;
import se.l4.silo.engine.index.search.locales.Locales;
import se.l4.silo.engine.index.search.query.QueryBuilder;
import se.l4.silo.engine.index.search.query.QueryBuilders;
import se.l4.silo.index.search.QueryClause;
import se.l4.silo.index.search.SearchHit;
import se.l4.silo.index.search.SearchIndexException;
import se.l4.silo.index.search.SearchIndexQuery;
import se.l4.silo.index.search.SearchResult;

public class SearchIndexQueryRunner<T>
	implements IndexQueryRunner<T, SearchIndexQuery<T, ?>>
{
	private static final long DEFAULT_OFFSET = 0;
	private static final long DEFAULT_LIMIT = 10;

	private final Locales locales;

	private final QueryBuilders queryParsers;

	private final IndexDefinitionImpl encounter;

	private final IndexSearcherManager searcherManager;

	private final TransactionValue<IndexSearcherHandle> handleValue;

	public SearchIndexQueryRunner(
		Locales locales,
		QueryBuilders queryBuilders,

		IndexDefinitionImpl encounter,

		IndexSearcherManager searcherManager
	)
	{
		this.locales = locales;
		this.queryParsers = queryBuilders;

		this.encounter = encounter;

		this.searcherManager = searcherManager;
		handleValue = v -> searcherManager.acquire();
	}

	@Override
	public void provideTransactionValues(
		Consumer<? super TransactionValue<?>> consumer
	)
	{
		consumer.accept(handleValue);
	}

	@Override
	public Mono<? extends FetchResult<?>> fetch(
		IndexQueryEncounter<? extends SearchIndexQuery<T, ?>, T> encounter
	)
	{
		return Mono.fromSupplier(() -> {
			IndexSearcherHandle handle = encounter.get(handleValue);

			ResultHitCollector<T> collector = new ResultHitCollector<>(encounter);

			query(
				handle,
				encounter.getQuery(),
				collector
			);

			return collector.create();
		});
	}

	@Override
	public Flux<?> stream(
		IndexQueryEncounter<? extends SearchIndexQuery<T, ?>, T> encounter
	)
	{
		// TODO Auto-generated method stub
		return null;
	}

	public void query(
		IndexSearcherHandle handle,
		SearchIndexQuery<T, ?> query,
		HitCollector<T> hitCollector
	)
	{
		try
		{
			if(query instanceof SearchIndexQuery.Limited)
			{
				SearchIndexQuery.Limited<T> limited = (SearchIndexQuery.Limited<T>) query;
				limitedSearch(limited, hitCollector, handle.getSearcher());
			}
			else
			{
				// TODO: Streaming via cursors
				throw new SearchIndexException("Unsupported type of query: " + query);
			}
		}
		catch(IOException e)
		{
			throw new StorageException("Unable to search; " + e.getMessage(), e);
		}
	}

	/**
	 * Perform a search for {@link SearchIndexQuery.Limited} applying
	 * pagination as needed.
	 *
	 * @param query
	 * @param hitCollector
	 * @param searcher
	 * @throws IOException
	 */
	private void limitedSearch(
		SearchIndexQuery.Limited<T> query,
		HitCollector<T> hitCollector,
		IndexSearcher searcher
	)
		throws IOException
	{
		long offset = query.getResultOffset().orElse(DEFAULT_OFFSET);
		long limit = query.getResultLimit().orElse(DEFAULT_LIMIT);

		Collector resultCollector;
		if(limit == 0)
		{
			// If limit is set to zero this is counting requests
			resultCollector = new TotalHitCountCollector();
		}
		else if(query.getSortOrder().isEmpty())
		{
			// No sorting, only collect the top scoring ones
			resultCollector = TopScoreDocCollector.create(
				(int) (offset + limit),
				Integer.MAX_VALUE
			);
		}
		else
		{
			// Sorting results
			Sort sort = createSort(query);
			resultCollector = TopFieldCollector.create(
				sort,
				(int) (offset + limit),
				null,
				Integer.MAX_VALUE
			);
		}

		search(query, searcher, resultCollector, limit > 0, true);

		long hits;
		if(limit == 0)
		{
			// No results requested, just collect the number of hits
			hits = ((TotalHitCountCollector) resultCollector).getTotalHits();
		}
		else
		{
			// Results requested, slice things up and load the data
			TopDocs docs = ((TopDocsCollector<?>) resultCollector).topDocs();
			hits = docs.totalHits.value;

			for(long i=offset, n=docs.scoreDocs.length; i<n; i++)
			{
				ScoreDoc d = docs.scoreDocs[(int) i];
				hitCollector.collect(searcher, d);
			}
		}

		// TODO: Support estimated totals
		hitCollector.metadata(hits, false);
	}

	private Sort createSort(SearchIndexQuery<T, ?> query)
	{
		SortField[] fields = query.getSortOrder()
			.collect(s -> {
				SearchFieldDefinition<?> fdef = encounter.getField(s.getField());

				// TODO: LocaleSupport?
				String name = encounter.sortValuesName(fdef, null);
				return fdef.getType().createSortField(name, s.isAscending());
			})
			.toArray(new SortField[0]);

		return new Sort(fields);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void search(
		SearchIndexQuery query,
		IndexSearcher searcher,
		Collector collector,
		boolean score,
		boolean withFacets
	)
		throws IOException
	{
		// TODO: Get the language of the query here
		LocaleSupport currentLocale = locales.getDefault();

		Query parsedQuery = createRootQuery(currentLocale, query.getClauses());
		// log.debug("Searching with query {}", parsedQuery);

		searcher.search(parsedQuery, collector);
	}

	private Query createRootQuery(
		LocaleSupport currentLocale,
		ListIterable<QueryClause> clauses
	)
		throws IOException
	{
		BooleanQuery.Builder result = new BooleanQuery.Builder();
		result.add(new ConstantScoreQuery(new DocValuesFieldExistsQuery("_:id")), Occur.MUST);

		for(QueryClause clause : clauses)
		{
			Query q = createQuery(currentLocale, clause);
			if(q instanceof BooleanQuery)
			{
				boolean hasShoulds = false;
				for(BooleanClause c : ((BooleanQuery) q).clauses())
				{
					if(c.getOccur() == BooleanClause.Occur.SHOULD)
					{
						hasShoulds = true;
						break;
					}
				}

				if(hasShoulds)
				{
					result.add(q, Occur.MUST);
				}
				else
				{
					for(BooleanClause c : ((BooleanQuery) q).clauses())
					{
						result.add(c);
					}
				}
			}
			else
			{
				result.add(q, Occur.MUST);
			}
		}

		return result.build();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Query createQuery(
		LocaleSupport queryLocale,
		QueryClause clause
	)
		throws IOException
	{
		QueryBuilder<?> qp = queryParsers.get(clause.getClass())
			.orElseThrow(() -> new SearchIndexException("Unknown type of query, got: " + clause));

		LocaleSupport defaultLanguage = locales.getDefault();
		return qp.parse(new QueryParserEncounterImpl(
			queryLocale,
			defaultLanguage,
			clause
		));
	}

	private static final long deserializeId(byte[] data)
	{
		return ((long) data[7] << 56)
			| ((long) data[6] & 0xff) << 48
			| ((long) data[5] & 0xff) << 40
			| ((long) data[4] & 0xff) << 32
			| ((long) data[3] & 0xff) << 24
			| ((long) data[2] & 0xff) << 16
			| ((long) data[1] & 0xff) << 8
			| ((long) data[0] & 0xff);
	}

	private static class ResultHitCollector<T>
		extends HitCollector<T>
	{
		private final MutableList<SearchHit<T>> results;
		private long totalHits;
		private boolean totalEstimated;

		public ResultHitCollector(
			IndexQueryEncounter<? extends SearchIndexQuery<T, ?>, T> encounter
		)
		{
			super(encounter);

			this.results = Lists.mutable.empty();
		}

		@Override
		public void searchHit(SearchHit<T> hit)
		{
			results.add(hit);
		}

		@Override
		public void metadata(
			long totalHits,
			boolean isTotalEstimated
		)
		{
			this.totalHits = totalHits;
			this.totalEstimated = isTotalEstimated;
		}

		@SuppressWarnings("unchecked")
		public SearchResult<T> create()
		{
			if(encounter.getQuery() instanceof SearchIndexQuery.Limited)
			{
				SearchIndexQuery.Limited<T> limited = (SearchIndexQuery.Limited<T>) encounter.getQuery();
				return new AbstractSearchResult.LimitedImpl<>(
					results.toImmutable(),
					totalHits,
					totalEstimated,
					limited.getResultOffset().orElse(DEFAULT_OFFSET),
					limited.getResultLimit().orElse(DEFAULT_LIMIT),
					null // TODO: Facets
				);
			}
			else
			{
				throw new SearchIndexException("Unsupported query: " + encounter);
			}
		}
	}

	private static abstract class HitCollector<T>
	{
		protected final IndexQueryEncounter<? extends SearchIndexQuery<T, ?>, T> encounter;
		private final Set<String> fields;

		public HitCollector(
			IndexQueryEncounter<? extends SearchIndexQuery<T, ?>, T> encounter
		)
		{
			this.encounter = encounter;

			this.fields = Collections.singleton("_:id");
		}

		public void collect(
			IndexSearcher searcher,
			ScoreDoc scoreDoc
		)
			throws IOException
		{
			// TODO: Switch this to using a StoredFieldVisitor
			// TODO: Support for highlighting

			Document doc = searcher.doc(scoreDoc.doc, fields);
			BytesRef idRef = doc.getBinaryValue("_:id");

			long id = deserializeId(idRef.bytes);
			T data = encounter.load(id);

			searchHit(new SearchHitImpl<>(data, scoreDoc.score));
		}

		public abstract void searchHit(SearchHit<T> hit);

		public abstract void metadata(
			long totalHits,
			boolean isTotalEstimated
		);
	}

	private class QueryParserEncounterImpl<C extends QueryClause>
		implements se.l4.silo.engine.index.search.query.QueryEncounter<C>
	{
		private final LocaleSupport currentLocaleSupport;
		private final LocaleSupport defaultLanguage;
		private final C data;

		public QueryParserEncounterImpl(
			LocaleSupport current,
			LocaleSupport defaultLanguage,
			C data
		)
		{
			this.currentLocaleSupport = current;
			this.defaultLanguage = defaultLanguage;
			this.data = data;
		}

		@Override
		public boolean isSpecificLanguage()
		{
			return currentLocaleSupport != defaultLanguage;
		}

		@Override
		public LocaleSupport currentLanguage()
		{
			return currentLocaleSupport == null ? defaultLanguage : currentLocaleSupport;
		}

		@Override
		public LocaleSupport defaultLanguage()
		{
			return defaultLanguage;
		}

		@Override
		public SearchIndexEncounter index()
		{
			return encounter;
		}

		@Override
		public C clause()
		{
			return data;
		}

		@Override
		public Query parse(QueryClause item)
			throws IOException
		{
			return createQuery(currentLocaleSupport, item);
		}
	}
}
