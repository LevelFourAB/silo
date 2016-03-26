package se.l4.silo.search;

public interface FacetQueryBuilder<Parent>
{
	FacetQueryBuilder<Parent> set(String key, String value);
	
	Parent done();
}
