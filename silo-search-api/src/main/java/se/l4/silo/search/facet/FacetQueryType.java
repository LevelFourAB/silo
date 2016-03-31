package se.l4.silo.search.facet;

import java.util.function.Function;

import se.l4.silo.search.FacetItem;
import se.l4.silo.search.FacetQueryBuilder;

public interface FacetQueryType<Parent, Builder extends FacetQueryBuilder<Parent>>
{
	Builder create(String id, Function<FacetItem, Parent> configReceiver);
}
