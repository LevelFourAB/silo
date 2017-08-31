package se.l4.silo.engine.search.builder;

import se.l4.silo.engine.search.SearchFieldType;

/**
 * Builder for fields.
 *
 * @author Andreas Holstenson
 *
 * @param
 */
public interface FieldBuilder<Parent>
{
	/**
	 * Return from building this field.
	 *
	 * @return
	 */
	Parent done();

	/**
	 * Set the type of field.
	 *
	 * @param type
	 * @return
	 */
	FieldBuilder<Parent> type(SearchFieldType type);

	/**
	 * Set that this field is language specific.
	 *
	 * @param language
	 * @return
	 */
	FieldBuilder<Parent> languageSpecific();

	/**
	 * Set if this field should be language specific.
	 *
	 * @param languageSpecific
	 * @return
	 */
	FieldBuilder<Parent> languageSpecific(boolean languageSpecific);

	/**
	 * Set if the field should be indexed.
	 *
	 * @param indexed
	 * @return
	 */
	FieldBuilder<Parent> indexed(boolean indexed);

	/**
	 * Set that the contents of the field should be stored. See {@link #stored(boolean)}.
	 *
	 * @return
	 */
	FieldBuilder<Parent> stored();

	/**
	 * Set if the contents of the field should be stored so that it may be
	 * retrieved during searching.
	 *
	 * @param store
	 * @return
	 */
	FieldBuilder<Parent> stored(boolean store);

	/**
	 * Set that the field should allow multiple values for the same document.
	 *
	 * @return
	 */
	FieldBuilder<Parent> multiValued();

	/**
	 * Set if the field should allow multiple values for the same document.
	 *
	 * @param multivalued
	 * @return
	 */
	FieldBuilder<Parent> multiValued(boolean multivalued);

	/**
	 * Set that the field should be prepared for fast highlighting. See {@link #highlighted(boolean)}.
	 *
	 * @return
	 */
	FieldBuilder<Parent> highlighted();

	/**
	 * Set that the field should be prepared for fast highlighting. This
	 * will turn on storing of extra information for terms.
	 *
	 * @param highlighted
	 * @return
	 */
	FieldBuilder<Parent> highlighted(boolean highlighted);

	/**
	 * Set that this field can be sorted on.
	 *
	 * @return
	 */
	FieldBuilder<Parent> sorted();

	/**
	 * Set that this field can be sorted on.
	 *
	 * @return
	 */
	FieldBuilder<Parent> sorted(boolean sorted);

	/**
	 * Set if values should be quickly accessible for this field. Used for things such as scoring and faceting.
	 *
	 * @return
	 */
	FieldBuilder<Parent> storeValues();

	/**
	 * Set if values should be quickly accessible for this field. Used for things such as scoring and faceting.
	 *
	 * @return
	 */
	FieldBuilder<Parent> storeValues(boolean stored);
}
