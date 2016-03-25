package se.l4.silo.engine.builder;

public interface SearchIndexBuilder<Parent>
	extends BuilderWithParent<Parent>
{
	/**
	 * Add the specified field to the index.
	 * 
	 * @param field
	 * @return
	 */
	SearchIndexBuilder<Parent> addField(String field);
}
