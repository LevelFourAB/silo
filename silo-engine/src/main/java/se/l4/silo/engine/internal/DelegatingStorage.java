package se.l4.silo.engine.internal;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.silo.DeleteResult;
import se.l4.silo.Entity;
import se.l4.silo.FetchResult;
import se.l4.silo.StorageException;
import se.l4.silo.StoreResult;
import se.l4.silo.engine.Snapshot;
import se.l4.silo.engine.Storage;
import se.l4.silo.query.Query;

/**
 * {@link Storage} that delegates to another instance. This is what is
 * exposed to {@link Entity entity implementations}. This allows the
 * {@link StorageEngine} to switch the underlying storage when needed,
 * for example when a {@link Snapshot} is being installed.
 */
public class DelegatingStorage<T>
	implements Storage<T>
{
	private volatile Storage<T> storage;

	public DelegatingStorage()
	{
	}

	public Storage<T> getStorage()
	{
		return storage;
	}

	public void setStorage(Storage<T> storage)
	{
		this.storage = storage;
	}

	private Mono<Storage<T>> storage()
	{
		return Mono.fromSupplier(() -> {
			if(storage == null)
			{
				throw new StorageException("Storage instance is currently locked and can not be used");
			}

			return storage;
		});
	};



	@Override
	public <ID> Mono<StoreResult<ID, T>> store(ID id, T instance)
	{
		return storage().flatMap(s -> storage.store(id, instance));
	}

	@Override
	public Mono<T> get(Object id)
	{
		return storage().flatMap(s -> s.get(id));
	}

	@Override
	public <ID> Mono<DeleteResult<ID, T>> delete(ID id)
	{
		return storage().flatMap(s -> s.delete(id));
	}

	@Override
	public <R, FR extends FetchResult<R>> Mono<FR> fetch(
		Query<T, R, FR> query
	)
	{
		return storage().flatMap(s -> s.fetch(query));
	}

	@Override
	public <R> Flux<R> stream(Query<T, R, ?> query)
	{
		return storage().flatMapMany(s -> s.stream(query));
	}

	@Override
	public Flux<T> stream()
	{
		return storage().flatMapMany(s -> storage.stream());
	}
}
