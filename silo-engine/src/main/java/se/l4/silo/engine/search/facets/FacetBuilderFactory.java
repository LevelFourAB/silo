package se.l4.silo.engine.search.facets;

import java.util.function.Function;

import se.l4.silo.engine.builder.BuilderWithParent;

public interface FacetBuilderFactory<Parent, T extends BuilderWithParent<Parent>>
{
	T create(Function<Facet<?>, Parent> instanceReceiver);
}
