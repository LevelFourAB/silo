package se.l4.silo.search;

import java.util.function.Function;

public interface FacetQueryBuilder<Parent>
{
	void setReceiver(String id, Function<FacetItem, Parent> configReceiver);
	
	Parent done();
}
