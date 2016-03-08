package se.l4.silo.engine.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.h2.mvstore.MVStore;

import se.l4.silo.StorageException;
import se.l4.silo.engine.Fields;
import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.QueryEngineCreationEncounter;
import se.l4.silo.engine.config.QueryEngineConfig;

/**
 * Implementation of {@link QueryEngineCreationEncounter}.
 * 
 * @author Andreas Holstenson
 *
 * @param <C>
 */
public class QueryEngineCreationEncounterImpl<C extends QueryEngineConfig>
	implements QueryEngineCreationEncounter<C>
{
	private final Path root;
	private final String name;
	private final C config;
	private final Fields fields;

	public QueryEngineCreationEncounterImpl(Path root, String name, C config, Fields fields)
	{
		this.root = root;
		this.name = name;
		this.config = config;
		this.fields = fields;
	}
	
	@Override
	public String getName()
	{
		return name;
	}
	
	@Override
	public C getConfig()
	{
		return config;
	}
	
	@Override
	public Fields getFields()
	{
		return fields;
	}
	
	@Override
	public Path resolveDataFile(Path path)
	{
		return root.resolve(path);
	}
	
	@Override
	public Path resolveDataFile(String name)
	{
		return root.resolve(name);
	}
	
	@Override
	public MVStoreManager openMVStore(String name)
	{
		try
		{
			// Make sure the root directory is created
			Files.createDirectories(root);
		}
		catch(IOException e)
		{
			throw new StorageException("Unable to create data directory: " + root + "; " + e.getMessage(), e);
		}
		
		MVStore store = new MVStore.Builder()
			.fileName(resolveDataFile(name).toString())
			.compress()
			.open();
		return new MVStoreManagerImpl(store);
	}
}
