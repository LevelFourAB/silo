package se.l4.silo.search.query;

import se.l4.silo.search.QueryItem;

public class ConstantScoreData
{
	private final float score;
	private final QueryItem subQuery;

	public ConstantScoreData(float score, QueryItem subQuery)
	{
		this.score = score;
		this.subQuery = subQuery;
	}
	
	public float getScore()
	{
		return score;
	}
	
	public QueryItem getSubQuery()
	{
		return subQuery;
	}
}
