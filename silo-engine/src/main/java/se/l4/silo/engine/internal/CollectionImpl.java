package se.l4.silo.engine.internal;

import java.util.function.Function;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.silo.DeleteResult;
import se.l4.silo.FetchResult;
import se.l4.silo.StoreResult;
import se.l4.silo.engine.LocalCollection;
import se.l4.silo.engine.index.LocalIndex;
import se.l4.silo.index.Query;

public class CollectionImpl<ID, T>
	implements LocalCollection<ID, T>
{
	private final String name;
	private final Function<T, ID> idSupplier;
	private final Storage<T> storage;

	public CollectionImpl(
		String name,
		Function<T, ID> idSupplier,
		Storage<T> storage
	)
	{
		this.name = name;
		this.idSupplier = idSupplier;
		this.storage = storage;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public long getReads()
	{
		return storage.getReads();
	}

	@Override
	public long getStores()
	{
		return storage.getStores();
	}

	@Override
	public long getDeletes()
	{
		return storage.getDeletes();
	}

	@Override
	public Mono<T> get(Object id)
	{
		return storage.get(id);
	}

	@Override
	public Mono<Boolean> contains(ID id)
	{
		return storage.contains(id);
	}

	@Override
	public Mono<StoreResult<ID, T>> store(T object)
	{
		ID id = idSupplier.apply(object);
		return storage.store(id, object);
	}

	@Override
	public Mono<DeleteResult<ID, T>> delete(ID id)
	{
		return storage.delete(id);
	}

	@Override
	public <R, FR extends FetchResult<R>> Mono<FR> fetch(
		Query<T, R, FR> query
	)
	{
		return storage.fetch(query);
	}

	@Override
	public <R> Flux<R> stream(Query<T, R, ?> query)
	{
		return storage.stream(query);
	}

	@Override
	public Mono<LocalIndex> index(String name)
	{
		return storage.getIndex(name);
	}

	@Override
	public Flux<LocalIndex> indexes()
	{
		return storage.indexes();
	}
}
