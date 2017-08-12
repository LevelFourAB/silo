package se.l4.silo.engine.search;

/**
 * Builder for adding fields to the index.
 *
 * @author Andreas Holstenson
 *
 */
public interface IndexedFieldBuilder
{
	/**
	 * Indicate that the field should include values.
	 *
	 * @return
	 */
	IndexedFieldBuilder withValues();

	/**
	 * Indicate if the field should have include values.
	 *
	 * @param values
	 * @return
	 */
	IndexedFieldBuilder withValues(boolean values);

	/**
	 * Indicate that the field can be sorted on.
	 *
	 * @return
	 */
	IndexedFieldBuilder withSorting();

	/**
	 * Indicate if the field should be able to be sorted.
	 *
	 * @param sorted
	 * @return
	 */
	IndexedFieldBuilder withSorting(boolean sorted);

	/**
	 * Indicate that the field can be highlighted.
	 *
	 * @return
	 */
	IndexedFieldBuilder withHighlighting();

	/**
	 * Indicate if the field should be able to highlight content.
	 *
	 * @param highlighted
	 * @return
	 */
	IndexedFieldBuilder withHighlighting(boolean highlighted);

	/**
	 * Indicate that this field is language specific.
	 *
	 * @return
	 */
	IndexedFieldBuilder languageSpecific();

	/**
	 * Indicate if the field is language specific.
	 *
	 * @param isSpecific
	 * @return
	 */
	IndexedFieldBuilder languageSpecific(boolean isSpecific);

	/**
	 * Add this field.
	 *
	 * @return
	 */
	FieldCreationEncounter add();
}
