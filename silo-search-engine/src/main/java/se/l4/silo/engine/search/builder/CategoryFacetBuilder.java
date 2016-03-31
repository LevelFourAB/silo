package se.l4.silo.engine.search.builder;

public interface CategoryFacetBuilder<Parent>
{
	CategoryFacetBuilder<Parent> field(String field);
	
	Parent done();
}
