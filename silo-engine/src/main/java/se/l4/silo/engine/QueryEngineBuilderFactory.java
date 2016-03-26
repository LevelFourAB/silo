package se.l4.silo.engine;

import java.util.function.Function;

import se.l4.silo.engine.builder.BuilderWithParent;
import se.l4.silo.engine.config.QueryEngineConfig;

public interface QueryEngineBuilderFactory<Parent, T extends BuilderWithParent<Parent>>
{
	T create(Function<QueryEngineConfig, Parent> configReceiver);
}
