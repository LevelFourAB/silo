package se.l4.silo.engine.internal.index;

import java.util.Arrays;

public class Result
{
	private final long id;
	final Object[] keys;
	final Object[] values;

	public Result(long id, Object[] key, Object[] values)
	{
		this.id = id;
		this.keys = key;
		this.values = values;
	}

	public long getId()
	{
		return id;
	}

	@Override
	public String toString()
	{
		return "Result [id=" + id + ", keys=" + Arrays.toString(keys) + ", values=" + Arrays.toString(values) + "]";
	}
}
