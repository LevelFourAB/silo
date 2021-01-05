package se.l4.silo.engine.internal.index;

import java.util.function.LongFunction;

import se.l4.silo.engine.TransactionExchange;
import se.l4.silo.engine.TransactionValue;
import se.l4.silo.engine.index.IndexQueryEncounter;
import se.l4.silo.index.Query;

/**
 * Implementation of {@link IndexQueryEncounter}.
 *
 * @param <T>
 */
public class IndexQueryEncounterImpl<D extends Query<T, ?, ?>, T>
	implements IndexQueryEncounter<D, T>
{
	private final TransactionExchange tx;
	private final D data;
	private final LongFunction<T> dataLoader;

	public IndexQueryEncounterImpl(
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
