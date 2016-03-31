package se.l4.silo.engine.search.builder;

/**
 * Builder for facets that group items by dates.
 * 
 * @author Andreas Holstenson
 *
 * @param <Parent>
 */
public interface DateFacetBuilder<Parent>
{
	/**
	 * Set the field this facet uses.
	 * 
	 * @param field
	 * @return
	 */
	DateFacetBuilder<Parent> field(String field);
	
	/**
	 * Build and add the facet.
	 * 
	 * @return
	 */
	Parent done();
}
