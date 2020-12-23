package se.l4.silo.engine.search.internal;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NRTCachingDirectory;
import org.apache.lucene.util.BytesRef;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.exobytes.streaming.StreamingFormat;
import se.l4.exobytes.streaming.StreamingInput;
import se.l4.exobytes.streaming.StreamingOutput;
import se.l4.exobytes.streaming.Token;
import se.l4.silo.FetchResult;
import se.l4.silo.StorageException;
import se.l4.silo.engine.QueryEncounter;
import se.l4.silo.engine.QueryEngine;
import se.l4.silo.engine.TransactionValue;
import se.l4.silo.engine.io.ExtendedDataInputStream;
import se.l4.silo.engine.io.ExtendedDataOutputStream;
import se.l4.silo.engine.search.LocaleSupport;
import se.l4.silo.engine.search.Locales;
import se.l4.silo.engine.search.SearchFieldDefinition;
import se.l4.silo.engine.search.SearchFieldType;
import se.l4.silo.engine.search.SearchIndexEncounter;
import se.l4.silo.engine.search.config.IndexCacheConfig;
import se.l4.silo.engine.search.config.IndexCommitConfig;
import se.l4.silo.engine.search.config.IndexReloadConfig;
import se.l4.silo.engine.search.query.QueryBuilder;
import se.l4.silo.engine.search.query.QueryBuilders;
import se.l4.silo.search.QueryClause;
import se.l4.silo.search.SearchHit;
import se.l4.silo.search.SearchIndexException;
import se.l4.silo.search.SearchIndexQuery;
import se.l4.silo.search.SearchResult;

/**
 * Query engine that maintains a Lucene index for an entity.
 */
public class SearchIndexQueryEngine<T>
	implements QueryEngine<T, SearchIndexQuery<T, ?>>
{
	private static final Logger log = LoggerFactory.getLogger(SearchIndexQueryEngine.class);
	private static final long DEFAULT_OFFSET = 0;
	private static final long DEFAULT_LIMIT = 10;

	private final String name;

	private final Locales locales;
	private final Function<T, Locale> localeSupplier;

	private final QueryBuilders queryParsers;

	private final IndexDefinitionImpl encounter;
	private final ImmutableMap<String, SearchFieldDefinition<T>> fields;

	private final Directory directory;
	private final IndexWriter writer;
	private final IndexSearcherManager searchManager;

	private final CommitPolicy commitPolicy;
	private final AtomicLong latestGeneration;

	private final TransactionValue<IndexSearcherHandle> handleValue;

	public SearchIndexQueryEngine(
		ScheduledExecutorService executor,
		String name,
		String uniqueName,
		Path directory,
		Locales locales,
		QueryBuilders queryParsers,
		IndexCommitConfig commitConfig,
		IndexReloadConfig reloadConfig,
		Function<T, Locale> localeSupplier,
		ImmutableMap<String, SearchFieldDefinition<T>> fields
	)
		throws IOException
	{
		this.name = name;
		this.locales = locales;
		this.queryParsers = queryParsers;

		this.localeSupplier = localeSupplier;
		this.fields = fields;

		encounter = new IndexDefinitionImpl(
			locales,
			(MapIterable) fields
		);

		// Create the directory implementation to use
		this.directory = createDirectory(FSDirectory.open(directory), reloadConfig.getCache());

		// Setup a basic configuration for the index writer
		IndexWriterConfig conf = new IndexWriterConfig(new SimpleAnalyzer());

		// Create the writer and searcher manager
		writer = new IndexWriter(this.directory, conf);

		latestGeneration = new AtomicLong();

		searchManager = new IndexSearcherManager(writer);
		handleValue = v -> searchManager.acquire();

		commitPolicy = new CommitPolicy(
			log,
			uniqueName,
			executor,
			writer,
			commitConfig.getMaxUpdates(),
			commitConfig.getMaxTime()
		);
	}

	private static Directory createDirectory(Directory directory, IndexCacheConfig config)
	{
		if(config.isActive())
		{
			return new NRTCachingDirectory(directory, config.getMaxMergeSize(), config.getMaxSize());
		}
		else
		{
			return directory;
		}
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public ListIterable<? extends TransactionValue<?>> getTransactionalValues()
	{
		return Lists.immutable.of(handleValue);
	}

	@Override
	public void close()
		throws IOException
	{
		commitPolicy.commit();
		commitPolicy.close();

		searchManager.close();

		writer.close();
		directory.close();
	}

	@Override
	public void generate(T data, ExtendedDataOutputStream rawOut)
		throws IOException
	{
		// Write a version tag
		rawOut.write(0);

		// Number of fields that are going to be written
		try(StreamingOutput out = StreamingFormat.CBOR.createOutput(rawOut))
		{
			// Write the locale of the item first
			Locale locale = localeSupplier.apply(data);
			if(locale == null)
			{
				locale = Locale.ENGLISH;
			}
			out.writeString(locale.toLanguageTag());

			// Write all of the extracted fields
			out.writeListStart(fields.size());

			for(SearchFieldDefinition<T> field : fields)
			{
				out.writeListStart(2);

				// Write the name of the field
				out.writeString(field.getName());

				// Get and write the value
				Object value = field.getSupplier().apply(data);
				if(value == null)
				{
					out.writeNull();
				}
				else
				{
					SearchFieldType type = field.getType();
					if(value instanceof RichIterable)
					{
						RichIterable<?> iterable = (RichIterable<?>) value;
						out.writeListStart(iterable.size());

						for(Object o : iterable)
						{
							type.write(o, out);
						}

						out.writeListEnd();
					}
					else if(value instanceof Collection)
					{
						Collection<?> iterable = (Collection<?>) value;
						out.writeListStart(iterable.size());

						for(Object o : iterable)
						{
							type.write(o, out);
						}

						out.writeListEnd();
					}
					else if(value instanceof Iterable)
					{
						out.writeListStart();

						for(Object o : (Iterable<?>) value)
						{
							type.write(o, out);
						}

						out.writeListEnd();
					}
					else
					{
						type.write(value, out);
					}
				}

				out.writeListEnd();
			}

			out.writeListEnd();
		}
	}

	@Override
	public void apply(long id, ExtendedDataInputStream rawIn)
		throws IOException
	{
		int version = rawIn.read();
		if(version != 0)
		{
			throw new StorageException("Unknown search index version encountered: " + version);
		}

		searchManager.willMutate();

		Document doc = new Document();

		BytesRef idRef = new BytesRef(serializeId(id));

		FieldType ft = new FieldType();
		ft.setStored(true);
		ft.setTokenized(false);
		ft.setIndexOptions(IndexOptions.DOCS);
		doc.add(new Field("_:id", idRef, ft));

		LocaleSupport defaultLangSupport = locales.get("en").get();

		try(StreamingInput in = StreamingFormat.CBOR.createInput(rawIn))
		{
			in.next(Token.VALUE);
			String rawLocale = in.readString();
			doc.add(new Field("_:lang", rawLocale, StringField.TYPE_STORED));

			// Resolve locale support
			LocaleSupport specificLanguageSupport = locales.getOrDefault(rawLocale);

			in.next(Token.LIST_START);

			while(in.peek() != Token.LIST_END)
			{
				in.next(Token.LIST_START);

				in.next(Token.VALUE);
				String fieldName = in.readString();

				SearchFieldDefinition field = encounter.getField(fieldName);
				if(field == null)
				{
					in.skipNext();
				}

				if(in.peek() == Token.NULL)
				{
					in.next();
					addField(doc, defaultLangSupport, specificLanguageSupport, field, null);
				}
				else if(in.peek() == Token.LIST_START)
				{
					// Stored a list of values, extract and index them
					in.next(Token.LIST_START);

					while(in.peek() != Token.LIST_END)
					{
						Object value = field.getType().read(in);
						addField(doc, defaultLangSupport, specificLanguageSupport, field, value);
					}

					in.next(Token.LIST_END);
				}
				else
				{
					Object value = field.getType().read(in);
					addField(doc, defaultLangSupport, specificLanguageSupport, field, value);
				}

				in.next(Token.LIST_END);
			}

			in.next(Token.LIST_END);
		}

		// Update the index
		Term idTerm = new Term("_:id", idRef);
		try
		{
			long generation = writer.updateDocument(idTerm, doc);
			latestGeneration.set(generation);
		}
		catch(IOException e)
		{
			throw new StorageException("Unable to update search index; " + e.getMessage(), e);
		}

		// Tell our commit policy that we have modified the index
		commitPolicy.indexModified();
	}

	private <V> void addField(
		Document document,
		LocaleSupport fallback,
		LocaleSupport current,
		SearchFieldDefinition<V> field,
		V object
	)
	{
		if(object == null)
		{
			String fieldName = encounter.nullName(field);
			FieldType ft = new FieldType();
			ft.setStored(false);
			ft.setIndexOptions(IndexOptions.DOCS);
			ft.setTokenized(false);
			ft.setOmitNorms(true);
			document.add(new Field(fieldName, BytesRef.EMPTY_BYTES, ft));
			return;
		}

		boolean needValues = encounter.getValueFields().contains(field.getName());

		if(field.isLanguageSpecific() && fallback != current)
		{
			// This field is locale specific and the provided locales are different
			IndexableField f1 = encounter.createIndexableField(current, field, object);
			document.add(f1);

			if(needValues)
			{
				document.add(encounter.createValuesField(current, field, object));
			}
		}

		// Create field on the fallback value
		IndexableField f2 = encounter.createIndexableField(fallback, field, object);
		document.add(f2);

		if(needValues)
		{
			document.add(encounter.createValuesField(fallback, field, object));
		}

		if(field.isSorted())
		{
			document.add(encounter.createSortingField(fallback, field, object));
		}
	}

	@Override
	public void delete(long id)
	{
		BytesRef idRef = new BytesRef(serializeId((id)));
		try
		{
			searchManager.willMutate();

			writer.deleteDocuments(new Term("_:id", idRef));
		}
		catch(IOException e)
		{
			throw new StorageException("Unable to delete from search index; " + e.getMessage(), e);
		}

		// Tell our commit policy that we have modified the index
		commitPolicy.indexModified();
	}

	@Override
	public Mono<? extends FetchResult<?>> fetch(
		QueryEncounter<? extends SearchIndexQuery<T, ?>, T> encounter
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
		QueryEncounter<? extends SearchIndexQuery<T, ?>, T> encounter
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
		/*
		FacetsCollector facetsCollector = null;
		if(withFacets && ! request.getFacetItems().isEmpty())
		{
			facetsCollector = new FacetsCollector();
			collector = MultiCollector.wrap(collector, facetsCollector);
		}
		*/

		// TODO: Get the language of the query here
		LocaleSupport currentLocale = locales.getDefault();

		Query parsedQuery = createRootQuery(currentLocale, query.getClauses());
		log.debug("Searching with query {}", parsedQuery);

		/*
		if(score && request.getScoring() != null)
		{
			ScoringItem item = request.getScoring();
			ScoringProvider provider = scoringProviders.get(item.getId());
			if(provider == null)
			{
				throw new StorageException("Unknown scoring provider with id " + item.getId());
			}

			query = new FunctionScoreQuery(query, new CustomScoreFunction<>(def, provider, item.getPayload()));
		}
		*/

		searcher.search(parsedQuery, collector);

		/*
		if(withFacets && ! request.getFacetItems().isEmpty())
		{
			FacetsCollector fc = facetsCollector;
			FacetsImpl results = new FacetsImpl();
			for(FacetItem fi : request.getFacetItems())
			{
				FacetDefinition fdef = def.getFacet(fi.getId());
				if(fdef == null)
				{
					throw new StorageException("Unknown facet: " + fi.getId());
				}

				List<FacetEntry> entries = fdef.getInstance().collect(new FacetCollectionEncounter()
				{
					@Override
					public Locale getLocale()
					{
						return null;
					}

					@Override
					public IndexDefinition getIndexDefinition()
					{
						return def;
					}

					@Override
					public FacetsCollector getCollector()
					{
						return fc;
					}

					@Override
					public IndexReader getIndexReader()
					{
						return searcher.getIndexReader();
					}

					@Override
					public IndexSearcher getIndexSearcher()
					{
						return searcher;
					}

					@Override
					public Object getQueryParameters()
					{
						return fi.getPayload();
					}
				});

				results.addAll(fi.getId(), entries);
			}

			return results;
		}

		return null;
		*/
	}

	private Query createRootQuery(
		LocaleSupport currentLocale,
		ListIterable<QueryClause> clauses
	)
		throws IOException
	{
		if(clauses.isEmpty())
		{
			return new MatchAllDocsQuery();
		}
		else if(clauses.size() == 1)
		{
			return createQuery(currentLocale, clauses.getFirst());
		}
		else
		{
			BooleanQuery.Builder result = new BooleanQuery.Builder();
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

	private static final byte[] serializeId(long id)
	{
		return new byte[] {
			(byte) id,
			(byte) (id >> 8),
			(byte) (id >> 16),
			(byte) (id >> 24),
			(byte) (id >> 32),
			(byte) (id >> 40),
			(byte) (id >> 48),
			(byte) (id >> 56)
		};
	}

	private static class ResultHitCollector<T>
		extends HitCollector<T>
	{
		private final MutableList<SearchHit<T>> results;
		private long totalHits;
		private boolean totalEstimated;

		public ResultHitCollector(
			QueryEncounter<? extends SearchIndexQuery<T, ?>, T> encounter
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
		protected final QueryEncounter<? extends SearchIndexQuery<T, ?>, T> encounter;
		private final Set<String> fields;

		public HitCollector(
			QueryEncounter<? extends SearchIndexQuery<T, ?>, T> encounter
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
		implements se.l4.silo.engine.search.query.QueryEncounter<C>
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
