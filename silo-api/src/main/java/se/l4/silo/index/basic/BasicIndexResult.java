package se.l4.silo.index.basic;

import se.l4.silo.FetchResult;
import se.l4.silo.results.LimitedFetchResult;
import se.l4.silo.results.SizeAwareResult;
import se.l4.silo.results.TotalAwareResult;

/**
 * Extension to {@link FetchResult} that is returned when fetching items via
 * a {@link BasicIndexQuery}.
 */
public interface BasicIndexResult<T>
	extends SizeAwareResult<T>, TotalAwareResult<T>, LimitedFetchResult<T>
{
}
