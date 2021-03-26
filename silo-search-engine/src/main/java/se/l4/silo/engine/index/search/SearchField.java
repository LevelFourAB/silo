package se.l4.silo.engine.index.search;

/**
 * Information about a field in an index.
 */
public interface SearchField<T, V>
{
	/**
	 * Get the definition used for this field.
	 *
	 * @return
	 */
	SearchFieldDef<T> getDefinition();

	/**
	 * Get if the field is indexed for searching.
	 *
	 * @return
	 */
	boolean isIndexed();

	/**
	 * Get if DocValues should be stored for the field.
	 *
	 * @return
	 */
	boolean isStoreDocValues();
}
