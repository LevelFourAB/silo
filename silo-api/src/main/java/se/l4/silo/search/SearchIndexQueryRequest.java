package se.l4.silo.search;

import java.util.ArrayList;
import java.util.List;

public class SearchIndexQueryRequest
{
	private int offset;
	private int limit;
	
	private String language;
	
	private List<QueryItem> queryItems;
	private List<FacetItem> facetItems;
	
	private List<SortItem> sortItems;
	
	private String scoring;
	
	public SearchIndexQueryRequest()
	{
		limit = 10;
		
		queryItems = new ArrayList<>();
		facetItems = new ArrayList<>();
		sortItems = new ArrayList<>();
	}
	
	public int getLimit()
	{
		return limit;
	}
	
	public void setLimit(int limit)
	{
		this.limit = limit;
	}
	
	public int getOffset()
	{
		return offset;
	}
	
	public void setOffset(int offset)
	{
		this.offset = offset;
	}
	
	public String getLanguage()
	{
		return language;
	}
	
	public void setLanguage(String language)
	{
		this.language = language;
	}
	
	public List<QueryItem> getQueryItems()
	{
		return queryItems;
	}
	
	public void addQueryItem(QueryItem item)
	{
		queryItems.add(item);
	}
	
	public List<FacetItem> getFacetItems()
	{
		return facetItems;
	}
	
	public void addFacetItem(FacetItem item)
	{
		facetItems.add(item);
	}
	
	public String getScoring()
	{
		return scoring;
	}
	
	public void setScoring(String scoring)
	{
		this.scoring = scoring;
	}
	
	public List<SortItem> getSortItems()
	{
		return sortItems;
	}
	
	public void addSortItem(String field, boolean ascending)
	{
		sortItems.add(new SortItem(field, ascending));
	}
}
