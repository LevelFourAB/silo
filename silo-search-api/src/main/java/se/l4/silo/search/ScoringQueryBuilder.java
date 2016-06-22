package se.l4.silo.search;

import java.util.function.Function;

public interface ScoringQueryBuilder<Parent>
{
	void setReceiver(Function<ScoringItem, Parent> configReceiver);
	
	Parent done();
}
