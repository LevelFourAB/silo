package se.l4.silo.engine.builder;

import se.l4.silo.engine.QueryEngineBuilderFactory;
import se.l4.silo.engine.QueryEngineFactory;

public interface HasQueryEngines<Self>
{
	/**
	 * Add a new index to to object entity. 
	 * @param name
	 * @return
	 */
	IndexBuilder<Self> addIndex(String name);
	
	/**
	 * Add a query engine to this object entity.
	 * 
	 * @param name
	 * @param type
	 * @return
	 */
	<T extends BuilderWithParent<Self>> T add(String name, QueryEngineFactory<T, ?> type);
	
	<T extends BuilderWithParent<Self>> T add(String name, QueryEngineBuilderFactory<Self, T> factory);
}
