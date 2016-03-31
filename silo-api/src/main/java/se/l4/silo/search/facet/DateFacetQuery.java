package se.l4.silo.search.facet;

import java.util.function.Function;

import se.l4.silo.search.FacetItem;
import se.l4.silo.search.FacetQueryBuilder;

public class DateFacetQuery<Parent>
	implements FacetQueryBuilder<Parent>
{
	private String id;
	private Function<FacetItem, Parent> configReceiver;
	private int count;

	public DateFacetQuery()
	{
		count = Integer.MAX_VALUE;
	}
	
	@Override
	public void setReceiver(String id, Function<FacetItem, Parent> configReceiver)
	{
		this.id = id;
		this.configReceiver = configReceiver;
	}
	
	/**
	 * Limit the number of returned facets to the given count.
	 * 
	 * @param count
	 * @return
	 */
	public DateFacetQuery<Parent> max(int count)
	{
		this.count = count;
		return this;
	}
	
	@Override
	public Parent done()
	{
		return configReceiver.apply(new FacetItem(id, new SimpleFacetQuery(count)));
	}
}
