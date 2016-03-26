package se.l4.silo.engine.builder;

import se.l4.silo.engine.search.builder.FieldBuilder;

public interface SearchIndexBuilder<Parent>
	extends BuilderWithParent<Parent>
{
	/**
	 * Starting adding a field to this index.
	 * 
	 * @param field
	 * @return
	 */
	FieldBuilder<SearchIndexBuilder<Parent>> addField(String field);
}
