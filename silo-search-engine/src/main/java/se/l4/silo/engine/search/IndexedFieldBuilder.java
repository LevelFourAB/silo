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
	 * Indicate that the field can be sorted on.
	 * 
	 * @return
	 */
	IndexedFieldBuilder withSorting();
	
	/**
	 * Indicate that the field can be highlighted.
	 * 
	 * @return
	 */
	IndexedFieldBuilder withHighlighting();
	
	/**
	 * Indicate that this field is language specific.
	 * 
	 * @return
	 */
	IndexedFieldBuilder languageSpecific();
	
	/**
	 * Add this field.
	 * 
	 * @return
	 */
	FieldCreationEncounter add();
}
