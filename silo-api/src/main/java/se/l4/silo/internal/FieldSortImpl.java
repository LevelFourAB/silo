package se.l4.silo.internal;

import java.util.Objects;

import se.l4.silo.index.FieldSort;

/**
 * Implementation of {@link FieldSort}.
 */
public class FieldSortImpl
	implements FieldSort
{
	private final String field;
	private final boolean ascending;

	public FieldSortImpl(
		String field,
		boolean ascending
	)
	{
		this.field = field;
		this.ascending = ascending;
	}

	@Override
	public String getField()
	{
		return field;
	}

	@Override
	public boolean isAscending()
	{
		return ascending;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(ascending, field);
	}

	@Override
	public boolean equals(Object obj)
	{
		if(this == obj) return true;
		if(obj == null) return false;
		if(getClass() != obj.getClass()) return false;
		FieldSortImpl other = (FieldSortImpl) obj;
		return ascending == other.ascending
			&& Objects.equals(field, other.field);
	}

	@Override
	public String toString()
	{
		return "FieldSort{field=" + field + ", ascending=" + ascending + "}";
	}
}
