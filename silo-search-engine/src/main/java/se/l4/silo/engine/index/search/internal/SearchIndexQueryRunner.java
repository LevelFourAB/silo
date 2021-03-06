package se.l4.silo.engine.index.search.internal;

import java.io.IOException;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsCollector.MatchingDocs;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.StoredFieldVisitor;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocValuesFieldExistsQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.TotalHitCountCollector;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.silo.FetchResult;
import se.l4.silo.StorageException;
import se.l4.silo.engine.TransactionValue;
import se.l4.silo.engine.collection.CountingCollector;
import se.l4.silo.engine.index.IndexQueryEncounter;
import se.l4.silo.engine.index.IndexQueryRunner;
import se.l4.silo.engine.index.search.SearchField;
import se.l4.silo.engine.index.search.SearchFieldDefinition;
import se.l4.silo.engine.index.search.SearchIndexEncounter;
import se.l4.silo.engine.index.search.facets.FacetCollectionEncounter;
import se.l4.silo.engine.index.search.facets.FacetCollector;
import se.l4.silo.engine.index.search.facets.FacetDef;
import se.l4.silo.engine.index.search.internal.facets.FacetResultImpl;
import se.l4.silo.engine.index.search.internal.facets.FacetValueImpl;
import se.l4.silo.engine.index.search.locales.LocaleSupport;
import se.l4.silo.engine.index.search.locales.Locales;
import se.l4.silo.engine.index.search.query.QueryBuilder;
import se.l4.silo.engine.index.search.query.QueryBuilders;
import se.l4.silo.index.search.QueryClause;
import se.l4.silo.index.search.SearchHit;
import se.l4.silo.index.search.SearchIndexException;
import se.l4.silo.index.search.SearchIndexQuery;
import se.l4.silo.index.search.SearchResult;
import se.l4.silo.index.search.facets.FacetQuery;
import se.l4.silo.index.search.facets.FacetResult;
import se.l4.silo.index.search.facets.FacetValue;

public class SearchIndexQueryRunner<T>
	implements IndexQueryRunner<T, SearchIndexQuery<T, ?>>
{
	private static final long DEFAULT_OFFSET = 0;
	private static final long DEFAULT_LIMIT = 10;

	private final Locales locales;

	private final QueryBuilders queryParsers;

	private final IndexDefinitionImpl<T> encounter;

	private final TransactionValue<IndexSearcherHandle> handleValue;

	public SearchIndexQueryRunner(
		Locales locales,
		QueryBuilders queryBuilders,

		IndexDefinitionImpl<T> encounter,

		IndexSearcherManager searcherManager
	)
	{
		this.locales = locales;
		this.queryParsers = queryBuilders;

		this.encounter = encounter;

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

			FullSearchResultCollector<T> collector = new FullSearchResultCollector<>(encounter);

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
		return fetch(encounter).flatMapMany(r -> r.stream());
	}

	public void query(
		IndexSearcherHandle handle,
		SearchIndexQuery<T, ?> query,
		ResultCollector<T> hitCollector
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
	 * @param resultCollector
	 * @param searcher
	 * @throws IOException
	 */
	private void limitedSearch(
		SearchIndexQuery.Limited<T> query,
		ResultCollector<T> resultCollector,
		IndexSearcher searcher
	)
		throws IOException
	{
		long offset = query.getResultOffset().orElse(DEFAULT_OFFSET);
		long limit = query.getResultLimit().orElse(DEFAULT_LIMIT);

		Collector luceneCollector;
		if(limit == 0)
		{
			// If limit is set to zero this is counting requests
			luceneCollector = new TotalHitCountCollector();
		}
		else if(query.getSortOrder().isEmpty())
		{
			// No sorting, only collect the top scoring ones
			luceneCollector = TopScoreDocCollector.create(
				(int) (offset + limit),
				Integer.MAX_VALUE
			);
		}
		else
		{
			// Sorting results
			Sort sort = createSort(query);
			luceneCollector = TopFieldCollector.create(
				sort,
				(int) (offset + limit),
				null,
				Integer.MAX_VALUE
			);
		}

		search(query, searcher, resultCollector, luceneCollector);

		long hits;
		if(limit == 0)
		{
			// No results requested, just collect the number of hits
			hits = ((TotalHitCountCollector) luceneCollector).getTotalHits();
		}
		else
		{
			// Results requested, slice things up and load the data
			TopDocs docs = ((TopDocsCollector<?>) luceneCollector).topDocs();
			hits = docs.totalHits.value;

			for(long i=offset, n=docs.scoreDocs.length; i<n; i++)
			{
				ScoreDoc d = docs.scoreDocs[(int) i];
				resultCollector.collect(searcher, d.doc, d.score);
			}
		}

		// TODO: Support estimated totals
		resultCollector.setHits(hits, false);
	}

	private Sort createSort(SearchIndexQuery<T, ?> query)
	{
		SortField[] fields = query.getSortOrder()
			.collect(s -> {
				SearchField<T, ?> field = encounter.getField(s.getField());

				// TODO: LocaleSupport?
				String name = encounter.sortValuesName(field.getDefinition(), null);
				return field.getDefinition().getType().createSortField(name, s.isAscending());
			})
			.toArray(new SortField[0]);

		return new Sort(fields);
	}

	/**
	 * Perform a search.
	 *
	 * @param query
	 * @param searcher
	 * @param collector
	 * @param facetCollector
	 * @throws IOException
	 */
	private void search(
		SearchIndexQuery<T, ?> query,
		IndexSearcher searcher,
		ResultCollector<T> collector,
		Collector luceneCollector
	)
		throws IOException
	{
		// TODO: Get the language of the query here
		LocaleSupport currentLocale = locales.getDefault();

		// Get the facets that are active for the query
		ListIterable<FacetHandler<?>> facets = query.getFacets()
			.collect(f -> new FacetHandler<>(encounter.getFacet(f.getId()), f));

		// Parse into Lucene query
		Query parsedQuery = createRootQuery(currentLocale, query.getClauses());
		// log.debug("Searching with query {}", parsedQuery);

		// Create a combined collector if facets are requested
		FacetsCollector facetHitCollector = null;
		if(collector.supportsFacets() && ! facets.isEmpty())
		{
			facetHitCollector = new FacetsCollector(false);

			luceneCollector = MultiCollector.wrap(luceneCollector, facetHitCollector);
		}

		// Perform the actual search
		searcher.search(parsedQuery, luceneCollector);

		if(facetHitCollector != null)
		{
			// Run through and collect the facets
			for(FacetHandler<?> handler : facets)
			{
				handler.prepareCollection(currentLocale);
			}

			// Perform the collection step
			for(MatchingDocs docs : facetHitCollector.getMatchingDocs())
			{
				for(FacetHandler<?> handler : facets)
				{
					handler.collectLeaf(docs.context.reader(), docs.bits);
				}
			}

			// Create the results
			for(FacetHandler<?> handler : facets)
			{
				collector.addFacet(handler.getResult());
			}
		}
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

	/**
	 * {@link ResultCollector} that creates instances of {@link SearchResult}.
	 */
	private static class FullSearchResultCollector<T>
		extends ResultCollector<T>
	{
		private final MutableList<SearchHit<T>> results;
		private final MutableList<FacetResult<?>> facets;

		private long totalHits;
		private boolean totalEstimated;

		public FullSearchResultCollector(
			IndexQueryEncounter<? extends SearchIndexQuery<T, ?>, T> encounter
		)
		{
			super(encounter);

			this.results = Lists.mutable.empty();
			this.facets = Lists.mutable.empty();
		}

		@Override
		public void searchHit(SearchHit<T> hit)
		{
			results.add(hit);
		}

		@Override
		public boolean supportsFacets()
		{
			return true;
		}

		@Override
		public void addFacet(FacetResult<?> facet)
		{
			facets.add(facet);
		}

		@Override
		public void setHits(
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
					facets.toImmutable()
				);
			}
			else
			{
				throw new SearchIndexException("Unsupported query: " + encounter);
			}
		}
	}

	/**
	 * Abstract collector of results.
	 */
	private static abstract class ResultCollector<T>
	{
		protected final IndexQueryEncounter<? extends SearchIndexQuery<T, ?>, T> encounter;

		private final Visitor<T> visitor;

		public ResultCollector(
			IndexQueryEncounter<? extends SearchIndexQuery<T, ?>, T> encounter
		)
		{
			this.encounter = encounter;

			this.visitor = new Visitor<>(encounter);
		}

		/**
		 * Collect a hit.
		 *
		 * @param searcher
		 * @param scoreDoc
		 * @throws IOException
		 */
		public void collect(
			IndexSearcher searcher,
			int docId,
			float score
		)
			throws IOException
		{
			// TODO: Support for highlighting
			searcher.doc(docId, visitor);

			SearchHit<T> hit = visitor.get(score);
			searchHit(hit);
		}

		/**
		 * Record a hit.
		 *
		 * @param hit
		 */
		public abstract void searchHit(SearchHit<T> hit);

		/**
		 * Get if this collector supports facets.
		 *
		 * @return
		 */
		public abstract boolean supportsFacets();

		/**
		 * Add a facet to this result.
		 *
		 * @param facet
		 */
		public abstract void addFacet(FacetResult<?> facet);

		/**
		 * Set metadata about the total number of hits.
		 *
		 * @param totalHits
		 * @param isTotalEstimated
		 */
		public abstract void setHits(
			long totalHits,
			boolean isTotalEstimated
		);
	}

	private static class Visitor<T>
		extends StoredFieldVisitor
	{
		private final IndexQueryEncounter<? extends SearchIndexQuery<T, ?>, T> encounter;

		private T data;

		public Visitor(
			IndexQueryEncounter<? extends SearchIndexQuery<T, ?>, T> encounter
		)
		{
			this.encounter = encounter;
		}

		@Override
		public Status needsField(FieldInfo fieldInfo)
			throws IOException
		{
			return "_:id".equals(fieldInfo.name) ? Status.YES : (data == null ? Status.NO : Status.STOP);
		}

		@Override
		public void binaryField(FieldInfo fieldInfo, byte[] value)
			throws IOException
		{
			long id = deserializeId(value);
			data = encounter.load(id);
		}

		public SearchHit<T> get(float score)
		{
			return new SearchHitImpl<>(data, score);
		}
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

	/**
	 * Handler class to simplify dealing with facets.
	 */
	private class FacetHandler<V>
	{
		private final FacetDef<T, V, ?> def;
		private final FacetQuery query;

		private FacetCollector<V> collector;
		private FacetCollectionEncounterImpl<V> collectionEncounter;

		public FacetHandler(
			FacetDef<T, V, ?> def,
			FacetQuery query
		)
		{
			this.def = def;
			this.query = query;
		}

		/**
		 * Prepare this facet for collecting values.
		 *
		 * @param locale
		 *   the current locale
		 */
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public void prepareCollection(LocaleSupport locale)
		{
			collector = ((FacetDef) def).createCollector(query);
			collectionEncounter = new FacetCollectionEncounterImpl<>(locale, query.getLimit());
		}

		/**
		 * Collect facets in the given leaf.
		 *
		 * @param reader
		 *   the reader
		 * @param docs
		 *   docs matching in the reader
		 */
		public void collectLeaf(LeafReader reader, DocIdSet docs)
			throws IOException
		{
			collectionEncounter.setLeaf(reader, docs);
			collector.collect(collectionEncounter);
		}

		/**
		 * Create the result.
		 *
		 * @return
		 */
		public FacetResult<V> getResult()
		{
			return new FacetResultImpl<>(def.getId(), collectionEncounter.getValues());
		}
	}

	/**
	 * Implementation of {@link FacetCollectionEncounter}.
	 */
	private class FacetCollectionEncounterImpl<V>
		implements FacetCollectionEncounter<V>
	{
		private final LocaleSupport locale;

		private LeafReader reader;
		private DocIdSet docs;

		private final CountingCollector<Object> collector;
		private Function<Object, V> mapper;

		public FacetCollectionEncounterImpl(
			LocaleSupport locale,
			OptionalInt count
		)
		{
			this.locale = locale;
			this.collector = count.isPresent()
				? CountingCollector.topK(count.getAsInt())
				: CountingCollector.sorted();
		}

		/**
		 * Set the leaf being read.
		 *
		 * @param reader
		 * @param docs
		 */
		public void setLeaf(
			LeafReader reader,
			DocIdSet docs
		)
		{
			this.reader = reader;
			this.docs = docs;
		}

		@Override
		public LeafReader getReader()
		{
			return reader;
		}

		@Override
		public DocIdSet getDocs()
		{
			return docs;
		}

		@Override
		public String getFieldName(SearchFieldDefinition<?> field)
		{
			return encounter.docValuesName(field, locale);
		}

		@Override
		public void collect(V value)
		{
			collector.offer(value);
		}

		@SuppressWarnings("unchecked")
		public Iterable<FacetValue<V>> getValues()
		{
			return collector.withCounts()
				.collect(e -> new FacetValueImpl<V>(
					mapper == null ? (V) e.getItem() : mapper.apply(e.getItem()),
					e.getCount())
				);
		}

		@Override
		public <NV> FacetCollectionEncounter<NV> map(Function<NV, V> toV)
		{
			this.mapper = (Function) toV;
			return (FacetCollectionEncounter) this;
		}
	}
}
