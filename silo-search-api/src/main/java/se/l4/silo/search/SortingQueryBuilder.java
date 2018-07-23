package se.l4.silo.search;


public interface SortingQueryBuilder<Parent>
{
	void setReceiver(Receiver<Parent> configReceiver);

	Parent ascending();

	Parent descending();

	interface Receiver<Parent>
	{
		Parent apply(boolean ascending, Object params);
	}
}
