package se.l4.silo.engine.builder;

import se.l4.silo.engine.QueryEngineFactory;

/**
 * Builder to create 
 * @author Andreas Holstenson
 *
 * @param <Parent>
 */
public interface StructuredEntityBuilder<Parent>
{
	/**
	 * Add a new index to to object entity. 
	 * @param name
	 * @return
	 */
	IndexBuilder<StructuredEntityBuilder<Parent>> addIndex(String name);
	
	/**
	 * Add a query engine to this object entity.
	 * 
	 * @param name
	 * @param type
	 * @return
	 */
	<T extends BuilderWithParent<StructuredEntityBuilder<Parent>>> T add(String name, QueryEngineFactory<T> type);
	
	/**
	 * Indicate that we are done building this entity.
	 * 
	 * @return
	 */
	Parent done();
}
