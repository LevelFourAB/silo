package se.l4.silo.engine.internal.search;

import java.util.function.Function;

import se.l4.silo.engine.builder.SearchIndexBuilder;
import se.l4.silo.engine.config.QueryEngineConfig;
import se.l4.silo.engine.config.SearchIndexConfig;

public class SearchIndexBuilderImpl<Parent>
	implements SearchIndexBuilder<Parent>
{
	private final Function<QueryEngineConfig, Parent> configReceiver;
	private final SearchIndexConfig config;
	
	public SearchIndexBuilderImpl(Function<QueryEngineConfig, Parent> configReceiver)
	{
		this.configReceiver = configReceiver;
		
		config = new SearchIndexConfig();
	}

	@Override
	public SearchIndexBuilder<Parent> addField(String field)
	{
		config.addField(new SearchIndexConfig.FieldConfig(field));
		return this;
	}

	@Override
	public Parent done()
	{
		return configReceiver.apply(config);
	}
}
