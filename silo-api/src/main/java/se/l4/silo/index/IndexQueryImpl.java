package se.l4.silo.index;

import se.l4.silo.FetchResult;
import se.l4.silo.StorageException;
import se.l4.silo.index.IndexQueryRequest.Op;
import se.l4.silo.query.IndexQuery;
import se.l4.silo.query.QueryRunner;

public class IndexQueryImpl<T>
	implements IndexQuery<T>
{
	private final QueryRunner<T, T> runner;
	private IndexQueryRequest request;
	private String field;

	public IndexQueryImpl(QueryRunner<T, T> runner)
	{
		this.runner = runner;

		request = new IndexQueryRequest();
	}

	@Override
	public FetchResult<T> run()
	{
		return runner.fetchResults(request, r -> r.getData());
	}

	@Override
	public IndexQuery<T> field(String name)
	{
		this.field = name;
		return this;
	}

	private IndexQuery<T> add(Op op, Object value)
	{
		if(field == null)
		{
			throw new StorageException("No field given");
		}

		request.addCritera(field, op, value);
		field = null;
		return this;
	}

	@Override
	public IndexQuery<T> isEqualTo(Object value)
	{
		return add(Op.EQUAL, value);
	}


	@Override
	public IndexQuery<T> isMoreThan(Number number)
	{
		return add(Op.MORE_THAN, number);
	}

	@Override
	public IndexQuery<T> isLessThan(Number number)
	{
		return add(Op.LESS_THAN, number);
	}

	@Override
	public IndexQuery<T> isLessThanOrEqualTo(Number number)
	{
		return add(Op.LESS_THAN_OR_EQUAL_TO, number);
	}

	@Override
	public IndexQuery<T> isMoreThanOrEqualTo(Number number)
	{
		return add(Op.MORE_THAN_OR_EQUAL_TO, number);
	}

	private IndexQuery<T> addSort(boolean ascending)
	{
		if(field == null)
		{
			throw new StorageException("No field given");
		}

		request.addSort(field, ascending);

		field = null;

		return this;
	}

	@Override
	public IndexQuery<T> sortAscending()
	{
		return addSort(true);
	}

	@Override
	public IndexQuery<T> sortDescending()
	{
		return addSort(false);
	}

	@Override
	public IndexQuery<T> sort(boolean ascending)
	{
		return addSort(ascending);
	}

	@Override
	public IndexQuery<T> reverseDefaultSort()
	{
		request.setReverseDefaultSort(true);
		return this;
	}

	@Override
	public IndexQuery<T> limit(long limit)
	{
		request.setLimit(limit);
		return this;
	}

	@Override
	public IndexQuery<T> offset(long offset)
	{
		request.setOffset(offset);
		return this;
	}

}
