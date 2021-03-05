package se.l4.silo.engine.index.search.types;

import se.l4.silo.engine.index.search.internal.types.StringFieldTypeBuilderImpl;

/**
 * {@link SearchFieldType} that works on instances of {@link String}.
 */
public interface StringFieldType
	extends SearchFieldType<String>, FacetableSearchFieldType<String>
{
	/**
	 * Start building a {@link StringFieldType}.
	 *
	 * @return
	 */
	static Builder create()
	{
		return new StringFieldTypeBuilderImpl();
	}

	interface Builder
	{
		/**
		 * Index string as a token.
		 *
		 * @return
		 *   instance for building token field
		 */
		TokenBuilder token();

		/**
		 * Index string as full text.
		 *
		 * @return
		 *   instance for building full text field
		 */
		FullTextBuilder fullText();
	}

	interface TokenBuilder
	{
		/**
		 * Build the type.
		 *
		 * @return
		 */
		StringFieldType build();
	}

	interface FullTextBuilder
	{
		/**
		 * Activate support for type ahead.
		 *
		 * @return
		 */
		FullTextBuilder withTypeAhead();

		/**
		 * Activate support for type ahead.
		 *
		 * @param typeAhead
		 *
		 * @return
		 */
		FullTextBuilder withTypeAhead(boolean typeAhead);

		/**
		 * Build the type.
		 *
		 * @return
		 */
		StringFieldType build();
	}
}
