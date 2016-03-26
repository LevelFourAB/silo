package se.l4.silo.search;

public interface FacetEntry
{
	String label();
	
	int count();
	
	Object data();
}
