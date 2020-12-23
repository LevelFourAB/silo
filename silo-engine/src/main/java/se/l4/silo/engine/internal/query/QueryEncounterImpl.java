package se.l4.silo.engine.internal.query;

import java.util.function.LongFunction;

import se.l4.silo.engine.QueryEncounter;
import se.l4.silo.engine.TransactionExchange;
import se.l4.silo.engine.TransactionValue;
import se.l4.silo.query.Query;

/**
 * Implementation of {@link QueryEncounter}.
 *
 * @param <T>
 */
public class QueryEncounterImpl<D extends Query<T, ?, ?>, T>
	implements QueryEncounter<D, T>
{
	private final TransactionExchange tx;
	private final D data;
	private final LongFunction<T> dataLoader;

	public QueryEncounterImpl(
		TransactionExchange tx,
		D data,
		LongFunction<T> dataLoader
	)
	{
		this.tx = tx;
		this.data = data;
		this.dataLoader = dataLoader;
	}

	@Override
	public D getQuery()
	{
		return data;
	}

	@Override
	public T load(long id)
	{
		return dataLoader.apply(id);
	}

	@Override
	public <V> V get(TransactionValue<V> value)
	{
		return tx.get(value);
	}
}
