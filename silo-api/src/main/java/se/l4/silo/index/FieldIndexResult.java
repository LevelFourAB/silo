package se.l4.silo.index;

import se.l4.silo.FetchResult;
import se.l4.silo.results.LimitedFetchResult;
import se.l4.silo.results.SizeAwareResult;
import se.l4.silo.results.TotalAwareResult;

/**
 * Extension to {@link FetchResult} that is returned when fetching items via
 * a {@link FieldIndexQuery}.
 */
public interface FieldIndexResult<T>
	extends SizeAwareResult<T>, TotalAwareResult<T>, LimitedFetchResult<T>
{
}
