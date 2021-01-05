package se.l4.silo.internal;

import java.util.function.Function;

import se.l4.silo.index.FieldSort;
import se.l4.silo.index.FieldSortBuilder;

/**
 * Implementation of {@link FieldSortBuilder}.
 */
public class FieldSortBuilderImpl<ReturnPath>
	implements FieldSortBuilder<ReturnPath>
{
	private final String field;
	private final Function<FieldSort, ReturnPath> receiver;

	public FieldSortBuilderImpl(
		String field,
		Function<FieldSort, ReturnPath> receiver
	)
	{
		this.field = field;
		this.receiver = receiver;
	}

	@Override
	public ReturnPath sort(boolean ascending)
	{
		return receiver.apply(FieldSort.create(field, ascending));
	}

	@Override
	public ReturnPath sortAscending()
	{
		return sort(true);
	}

	@Override
	public ReturnPath sortDescending()
	{
		return sort(false);
	}
}
