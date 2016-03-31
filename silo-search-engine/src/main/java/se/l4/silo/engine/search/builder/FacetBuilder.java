package se.l4.silo.engine.search.builder;

import se.l4.silo.engine.builder.BuilderWithParent;
import se.l4.silo.engine.search.facets.Facet;

/**
 * Builder for instances of {@link Facet}.
 * 
 * @author Andreas Holstenson
 *
 * @param <Parent>
 */
public interface FacetBuilder<Parent>
{
	/**
	 * Start adding a new facet for dates.
	 * 
	 * @return
	 */
	DateFacetBuilder<Parent> date();
	
	/**
	 * Start adding a new facet for categories.
	 * 
	 * @return
	 */
	CategoryFacetBuilder<Parent> category();
	
	<B extends BuilderWithParent<Parent>> B ofType(Facet<?> f);
}
