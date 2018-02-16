package se.l4.silo.search;

import java.util.ArrayList;
import java.util.List;

public class SearchIndexQueryRequest
{
	private long offset;
	private long limit;
	
	private String language;
	
	private List<QueryItem> queryItems;
	private List<FacetItem> facetItems;
	
	private List<SortItem> sortItems;
	
	private ScoringItem scoring;
	private boolean waitForLatest;
	
	public SearchIndexQueryRequest()
	{
		limit = 10;
		
		queryItems = new ArrayList<>();
		facetItems = new ArrayList<>();
		sortItems = new ArrayList<>();
	}
	
	public boolean isWaitForLatest()
	{
		return waitForLatest;
	}
	
	public void setWaitForLatest(boolean waitForLatest)
	{
		this.waitForLatest = waitForLatest;
	}
	
	public long getLimit()
	{
		return limit;
	}
	
	public void setLimit(long limit)
	{
		this.limit = limit;
	}
	
	public long getOffset()
	{
		return offset;
	}
	
	public void setOffset(long offset)
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
	
	public ScoringItem getScoring()
	{
		return scoring;
	}
	
	public void setScoring(ScoringItem scoring)
	{
		this.scoring = scoring;
	}
	
	public List<SortItem> getSortItems()
	{
		return sortItems;
	}
	
	public void addSortItem(String field, boolean ascending, Object params)
	{
		sortItems.add(new SortItem(field, ascending, params));
	}

	
}
