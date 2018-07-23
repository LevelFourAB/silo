package se.l4.silo.search.query;

import se.l4.commons.serialization.Expose;
import se.l4.commons.serialization.ReflectionSerializer;
import se.l4.commons.serialization.Use;

@Use(ReflectionSerializer.class)
public class RangeQueryData
{
	@Expose
	private final String field;

	@Expose
	private final long from;

	@Expose
	private final long to;

	public RangeQueryData(@Expose("field") String field, @Expose("from") long from, @Expose("to") long to)
	{
		this.field = field;
		this.from = from;
		this.to = to;
	}

	public String getField()
	{
		return field;
	}

	public long getFrom()
	{
		return from;
	}

	public long getTo()
	{
		return to;
	}
}
