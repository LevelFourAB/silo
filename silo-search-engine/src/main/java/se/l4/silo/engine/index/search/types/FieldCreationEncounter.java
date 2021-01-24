package se.l4.silo.engine.index.search.types;

import org.apache.lucene.index.IndexableField;

import se.l4.silo.engine.index.search.locales.LocaleSupport;

public interface FieldCreationEncounter<T>
{
	/**
	 * Get the value to store.
	 *
	 * @return
	 */
	T getValue();

	/**
	 * Get if the value should be indexed for searching.
	 *
	 * @return
	 */
	boolean isIndexed();

	/**
	 * Get if the value should be stored.
	 *
	 * @return
	 */
	boolean isStored();

	/**
	 * Get if the value should support highlighting.
	 *
	 * @return
	 */
	boolean isHighlighted();

	/**
	 * Get if the value should be sortable.
	 *
	 * @return
	 */
	boolean isSorted();

	/**
	 * Get if the value should be
	 * @return
	 */
	boolean isStoreDocValues();

	/**
	 * Get the current locale.
	 *
	 * @return
	 */
	LocaleSupport getLocale();

	/**
	 * Get under which names a doc values field is stored.
	 *
	 * @return
	 */
	String docValuesName();

	/**
	 * Get under which names a sort values field is stored.
	 *
	 * @return
	 */
	String sortValuesName();

	/**
	 * Get under which name this field is stored for the current locale.
	 *
	 * @return
	 */
	String name();

	/**
	 * Get under which name this field is stored for the current locale.
	 *
	 * @param variant
	 * @return
	 */
	String name(String variant);

	/**
	 * Emit an {@link IndexableField} that should be stored.
	 *
	 * @param field
	 */
	void emit(IndexableField field);
}
