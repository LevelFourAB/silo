package se.l4.silo.engine.internal.binary;

import se.l4.silo.DeleteResult;
import se.l4.silo.FetchResult;
import se.l4.silo.StoreResult;
import se.l4.silo.binary.BinaryEntity;
import se.l4.silo.binary.BinaryEntry;
import se.l4.silo.engine.Storage;
import se.l4.silo.engine.internal.BinaryEntryImpl;
import se.l4.ylem.io.Bytes;

/**
 * Implementation of {@link BinaryEntity}.
 *
 * @author Andreas Holstenson
 *
 */
public class BinaryEntityImpl
	implements BinaryEntity
{
	private final String name;
	private final Storage storage;

	public BinaryEntityImpl(String name, Storage storage)
	{
		this.name = name;
		this.storage = storage;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public StoreResult store(Object id, Bytes bytes)
	{
		return storage.store(id, bytes);
	}

	@Override
	public DeleteResult delete(Object id)
	{
		return storage.delete(id);
	}

	@Override
	public FetchResult<BinaryEntry> get(Object id)
	{
		Bytes data = storage.get(id);
		if(data == null)
		{
			return FetchResult.empty();
		}

		return FetchResult.single(new BinaryEntryImpl(id, null, data));
	}
}
