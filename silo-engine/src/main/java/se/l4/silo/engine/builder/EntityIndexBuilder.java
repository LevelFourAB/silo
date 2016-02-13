package se.l4.silo.engine.builder;

/**
 * Builder for instances of {@link EntityIndex}.
 * 
 * @author Andreas Holstenson
 *
 */
public interface EntityIndexBuilder<R>
{
	/**
	 * Add the specified field to the index.
	 * 
	 * @param field
	 * @return
	 */
	EntityIndexBuilder<R> addField(String field);
	
	/**
	 * Enable sorting on the specified field.
	 * 
	 * @param field
	 * @return
	 */
	EntityIndexBuilder<R> addSortField(String field);
	
	/**
	 * Add the built field and continue building the entity.
	 * 
	 * @return
	 */
	R add();
}
