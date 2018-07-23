package se.l4.silo.search.facet;

/**
 * Simple parameters needed by certain facets.
 *
 * @author Andreas Holstenson
 *
 */
public class SimpleFacetQuery
{
	private final int count;

	public SimpleFacetQuery(int count)
	{
		this.count = count > 0 ? count : 10;
	}

	/**
	 * Get maximum number of facets to return.
	 *
	 * @return
	 */
	public int getCount()
	{
		return count;
	}
}
