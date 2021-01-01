package se.l4.silo.engine.internal.mvstore;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVMap.Builder;
import org.h2.mvstore.MVStore;
import org.h2.store.fs.FileChannelInputStream;

import se.l4.silo.engine.MVStoreManager;
import se.l4.silo.engine.Snapshot;
import se.l4.silo.engine.types.DataTypeAdapter;
import se.l4.silo.engine.types.FieldType;

/**
 * Implementation of {@link MVStoreManager}.
 *
 * @author Andreas Holstenson
 *
 */
public class MVStoreManagerImpl
	implements MVStoreManager
{
	private final ScheduledExecutorService executorService;
	private final MVStore.Builder builder;

	private final MVStore store;
	private final AtomicLong snapshotsOpen;

	private volatile Future<?> future;
	private final CopyOnWriteArrayList<CommitAction> commitActions;

	public MVStoreManagerImpl(
		ScheduledExecutorService executorService,
		MVStore.Builder builder
	)
	{
		this.executorService = executorService;
		this.builder = builder;
		store = builder.open();

		snapshotsOpen = new AtomicLong();
		commitActions = new CopyOnWriteArrayList<>();
	}

	public MVStore getStore()
	{
		return store;
	}

	@Override
	public void registerCommitAction(CommitAction action)
	{
		if(future == null)
		{
			future = executorService.scheduleAtFixedRate(this::runCommitActions, 30, 30, TimeUnit.SECONDS);
		}
	}

	private void runCommitActions()
	{
		for(CommitAction ac : commitActions)
		{
			ac.preCommit();
		}

		store.commit();

		for(CommitAction ac : commitActions)
		{
			ac.afterCommit();
		}
	}

	@Override
	public <K, V> MVMap<K, V> openMap(String name, FieldType<K> key, FieldType<V> value)
	{
		return store.openMap(name, new MVMap.Builder<K, V>()
			.keyType(new DataTypeAdapter(key))
			.valueType(new DataTypeAdapter(value)));
	}

	@Override
	public <K, V> MVMap<K, V> openMap(String name, Builder<K, V> builder)
	{
		return store.openMap(name, builder);
	}

	@Override
	public VersionHandle acquireVersionHandle()
	{
		MVStore.TxCounter counter = store.registerVersionUsage();
		return new VersionHandle()
		{
			@Override
			public long getVersion()
			{
				return counter.version;
			}

			@Override
			public void release()
			{
				store.deregisterVersionUsage(counter);
			}
		};
	}

	@Override
	public Snapshot createSnapshot()
	{
		snapshotsOpen.incrementAndGet();
		store.commit();
		store.setReuseSpace(false);
		return new Snapshot()
		{
			private boolean closed = false;

			@Override
			public InputStream asStream()
				throws IOException
			{
				if(store.getReuseSpace())
				{
					throw new IOException("Can not use this snapshot, the underlying storage might be in an incosistent state");
				}

				if(closed)
				{
					throw new IOException("This snapshot has already been closed");
				}

				return new FileChannelInputStream(
					store.getFileStore().getFile(),
					false
				);
			}

			@Override
			public void close()
				throws IOException
			{
				if(closed) return;

				closed = true;
				if(snapshotsOpen.decrementAndGet() == 0)
				{
					store.setReuseSpace(true);
				}
			}
		};
	}

	@Override
	public void close()
		throws IOException
	{
		if(future != null)
		{
			future.cancel(false);
		}

		store.close();
	}

	public void compact(long targetTime)
	{
		if(snapshotsOpen.get() > 0)
		{
			return;
		}

		store.compactFile(targetTime);
	}
}
