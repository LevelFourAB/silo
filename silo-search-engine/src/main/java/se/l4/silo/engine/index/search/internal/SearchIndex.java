package se.l4.silo.engine.index.search.internal;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Function;

import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NRTCachingDirectory;
import org.eclipse.collections.api.RichIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.scheduler.Scheduler;
import se.l4.silo.engine.index.Index;
import se.l4.silo.engine.index.IndexDataGenerator;
import se.l4.silo.engine.index.IndexDataUpdater;
import se.l4.silo.engine.index.IndexQueryRunner;
import se.l4.silo.engine.index.search.SearchFieldDefinition;
import se.l4.silo.engine.index.search.config.IndexCacheConfig;
import se.l4.silo.engine.index.search.config.IndexCommitConfig;
import se.l4.silo.engine.index.search.facets.FacetDef;
import se.l4.silo.engine.index.search.locales.Locales;
import se.l4.silo.engine.index.search.query.QueryBuilders;
import se.l4.silo.index.search.SearchIndexQuery;

/**
 * Query engine that maintains a Lucene index for an entity.
 */
public class SearchIndex<T>
	implements Index<T, SearchIndexQuery<T, ?>>
{
	private static final Logger log = LoggerFactory.getLogger(SearchIndex.class);

	private final String name;

	private final Directory directory;
	private final IndexWriter writer;
	private final IndexSearcherManager searcherManager;
	private final CommitManager commitManager;

	private final SearchIndexDataGenerator<T> dataGenerator;
	private final SearchIndexDataUpdater dataUpdater;
	private final SearchIndexQueryRunner<T> queryRunner;

	public SearchIndex(
		Scheduler scheduler,
		String name,
		String uniqueName,
		Path directory,
		Locales locales,
		QueryBuilders queryBuilders,
		IndexCommitConfig commitConfig,
		IndexCacheConfig cacheConfig,
		Function<T, Locale> localeSupplier,
		RichIterable<SearchFieldDefinition<T>> fields,
		RichIterable<FacetDef<T, ?, ?>> facets
	)
		throws IOException
	{
		this.name = name;

		IndexDefinitionImpl encounter = new IndexDefinitionImpl(
			locales,
			fields,
			facets
		);

		// Create the directory implementation to use
		this.directory = createDirectory(FSDirectory.open(directory), cacheConfig);

		// Setup a basic configuration for the index writer
		IndexWriterConfig conf = new IndexWriterConfig(new SimpleAnalyzer());

		// Create the writer and searcher manager
		writer = new IndexWriter(this.directory, conf);

		searcherManager = new IndexSearcherManager(writer);

		commitManager = new CommitManager(
			log,
			uniqueName,
			scheduler,
			writer,
			commitConfig.getMaxUpdates(),
			commitConfig.getMaxTime().toMillis(),
			searcherManager
		);

		dataGenerator = new SearchIndexDataGenerator<>(localeSupplier, encounter.getFields());

		dataUpdater = new SearchIndexDataUpdater(
			locales,
			encounter,
			writer,
			searcherManager,
			commitManager
		);

		queryRunner = new SearchIndexQueryRunner<>(
			locales,
			queryBuilders,
			encounter,
			searcherManager
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
	public void close()
		throws IOException
	{
		commitManager.commit();
		commitManager.close();

		searcherManager.close();

		writer.close();
		directory.close();
	}

	@Override
	public IndexDataGenerator<T> getDataGenerator()
	{
		return dataGenerator;
	}

	@Override
	public IndexDataUpdater getDataUpdater()
	{
		return dataUpdater;
	}

	@Override
	public IndexQueryRunner<T, SearchIndexQuery<T, ?>> getQueryRunner()
	{
		return queryRunner;
	}
}
