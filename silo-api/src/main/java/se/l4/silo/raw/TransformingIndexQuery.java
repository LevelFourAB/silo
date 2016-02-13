package se.l4.silo.raw;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import se.l4.silo.FetchResult;

public class TransformingIndexQuery<T>
	implements EntityIndexQuery<T>
{
	private EntityIndexQuery query;
	private Function func;

	public <O> TransformingIndexQuery(EntityIndexQuery<O> query, Function<O, T> func)
	{
		this.query = query;
		this.func = func;
	}
	
	@Override
	public FetchResult<T> run()
	{
		return query.run().transform(func); 
	}
	
	@Override
	public CompletableFuture<FetchResult<T>> runAsync()
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public EntityIndexQuery<T> field(String name)
	{
		query.field(name);
		return this;
	}
	
	@Override
	public EntityIndexQuery<T> isEqualTo(Object value)
	{
		query.isEqualTo(value);
		return this;
	}
	
	@Override
	public EntityIndexQuery<T> isLessThan(Number number)
	{
		query.isLessThan(number);
		return this;
	}
	
	@Override
	public EntityIndexQuery<T> isLessThanOrEqualTo(Number number)
	{
		query.isLessThanOrEqualTo(number);
		return this;
	}
	
	@Override
	public EntityIndexQuery<T> isMoreThan(Number number)
	{
		query.isMoreThan(number);
		return this;
	}
	
	@Override
	public EntityIndexQuery<T> isMoreThanOrEqualTo(Number number)
	{
		query.isMoreThanOrEqualTo(number);
		return this;
	}
	
	@Override
	public EntityIndexQuery<T> sortAscending()
	{
		query.sortAscending();
		return this;
	}
	
	@Override
	public EntityIndexQuery<T> sortDescending()
	{
		query.sortDescending();
		return this;
	}
	
	@Override
	public EntityIndexQuery<T> limit(int limit)
	{
		query.limit(limit);
		return this;
	}
	
	@Override
	public EntityIndexQuery<T> offset(int offset)
	{
		query.offset(offset);
		return this;
	}
	
	@Override
	public EntityIndexQuery<T> returnCount()
	{
		query.returnCount();
		return this;
	}
	
	@Override
	public EntityIndexQuery<T> multipleOr()
	{
		query.multipleOr();
		return this;
	}
}
