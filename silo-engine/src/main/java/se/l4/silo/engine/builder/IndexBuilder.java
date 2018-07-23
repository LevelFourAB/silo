package se.l4.silo.engine.builder;

/**
 * Builder to create indexes for entities.
 *
 * @author Andreas Holstenson
 *
 */
public interface IndexBuilder<R>
	extends BuilderWithParent<R>
{
	/**
	 * Add the specified field to the index.
	 *
	 * @param field
	 * @return
	 */
	IndexBuilder<R> addField(String field);

	/**
	 * Enable sorting on the specified field.
	 *
	 * @param field
	 * @return
	 */
	IndexBuilder<R> addSortField(String field);
}
