package se.l4.silo.engine.internal.query;

import java.util.Iterator;
import java.util.Map;

import se.l4.silo.FetchResult;
import se.l4.silo.query.QueryFetchResult;

/**
 * Implementation of {@link QueryFetchResult} that delegates to a {@link FetchResult}
 * and just provides some metadata.
 *
 * @author Andreas Holstenson
 *
 * @param <T>
 */
public class DelegatingQueryFetchResult<T>
	implements QueryFetchResult<T>
{
	private final Map<String, Object> metadata;
	private final FetchResult<T> fr;

	public DelegatingQueryFetchResult(FetchResult<T> fr, Map<String, Object> metadata)
	{
		this.fr = fr;
		this.metadata = metadata;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <M> M getMetadata(String key)
	{
		return (M) metadata.get(key);
	}

	@Override
	public long getSize()
	{
		return fr.getSize();
	}

	@Override
	public long getOffset()
	{
		return fr.getOffset();
	}

	@Override
	public long getLimit()
	{
		return fr.getLimit();
	}

	@Override
	public Iterator<T> iterator()
	{
		return fr.iterator();
	}

	@Override
	public long getTotal()
	{
		return fr.getTotal();
	}

	@Override
	public boolean isEmpty()
	{
		return fr.isEmpty();
	}

	@Override
	public void close()
	{
		fr.close();
	}
}
