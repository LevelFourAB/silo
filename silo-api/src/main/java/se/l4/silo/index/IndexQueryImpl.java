package se.l4.silo.index;

import se.l4.silo.FetchResult;
import se.l4.silo.IndexQuery;
import se.l4.silo.QueryRunner;

public class IndexQueryImpl<T>
	implements IndexQuery<T>
{
	private final QueryRunner<T> runner;

	public IndexQueryImpl(QueryRunner<T> runner)
	{
		this.runner = runner;
	}

	@Override
	public FetchResult<T> run()
	{
		return runner.fetchResults("hello!");
	}

	@Override
	public IndexQuery<T> field(String name)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IndexQuery<T> multipleOr()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IndexQuery<T> isEqualTo(Object value)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IndexQuery<T> isMoreThan(Number number)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IndexQuery<T> isLessThan(Number number)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IndexQuery<T> isLessThanOrEqualTo(Number number)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IndexQuery<T> isMoreThanOrEqualTo(Number number)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IndexQuery<T> sortAscending()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IndexQuery<T> sortDescending()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IndexQuery<T> limit(int limit)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IndexQuery<T> offset(int offset)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IndexQuery<T> returnCount()
	{
		// TODO Auto-generated method stub
		return null;
	}

}
