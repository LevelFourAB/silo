package se.l4.silo.engine.internal.index;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import se.l4.silo.engine.builder.IndexBuilder;
import se.l4.silo.engine.config.IndexConfig;
import se.l4.silo.engine.config.QueryEngineConfig;

public class IndexQueryBuilderImpl<R>
	implements IndexBuilder<R>
{
	private final Function<QueryEngineConfig, R> configReceiver;
	private final List<String> fields;
	private final List<String> sortFields;
	
	public IndexQueryBuilderImpl(Function<QueryEngineConfig, R> configReceiver)
	{
		this.configReceiver = configReceiver;
		
		fields = new ArrayList<>();
		sortFields = new ArrayList<>();
	}

	@Override
	public IndexBuilder<R> addField(String field)
	{
		fields.add(field);
		return this;
	}

	@Override
	public IndexBuilder<R> addSortField(String field)
	{
		sortFields.add(field);
		return this;
	}

	@Override
	public R done()
	{
		IndexConfig config = new IndexConfig(
			fields.toArray(new String[fields.size()]),
			sortFields.toArray(new String[sortFields.size()])
		);
		return configReceiver.apply(config);
	}
}
