package se.l4.silo.search.query;

import se.l4.silo.search.QueryItem;
import se.l4.silo.search.QueryPart;
import se.l4.silo.search.QueryWithSubquery;

public class ConstantScoreQuery<R>
	extends AbstractQueryPart<R>
	implements QueryWithSubquery<ConstantScoreQuery<R>, R>
{
	private final float score;

	public ConstantScoreQuery(float score)
	{
		this.score = score;
	}
	
	@Override
	public void addQuery(QueryItem item)
	{
		receiver.addQuery(new QueryItem("constantScore", new ConstantScoreData(score, item)));
	}
	
	@Override
	public <P extends QueryPart<R>> P query(P q)
	{
		q.parent(parent, this);
		return q;
	}
}
