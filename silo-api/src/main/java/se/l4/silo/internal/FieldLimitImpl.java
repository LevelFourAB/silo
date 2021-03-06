package se.l4.silo.internal;

import java.util.Objects;

import se.l4.silo.index.FieldLimit;
import se.l4.silo.index.Matcher;

/**
 * Implementation of {@link FieldLimit}.
 */
public class FieldLimitImpl<V>
	implements FieldLimit<V>
{
	private final String field;
	private final Matcher<V> matcher;

	public FieldLimitImpl(
		String field,
		Matcher<V> matcher
	)
	{
		this.field = field;
		this.matcher = matcher;
	}

	@Override
	public String getField()
	{
		return field;
	}

	@Override
	public Matcher<V> getMatcher()
	{
		return matcher;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(field, matcher);
	}

	@Override
	public boolean equals(Object obj)
	{
		if(this == obj) return true;
		if(obj == null) return false;
		if(getClass() != obj.getClass()) return false;
		FieldLimitImpl other = (FieldLimitImpl) obj;
		return Objects.equals(field, other.field)
			&& Objects.equals(matcher, other.matcher);
	}

	@Override
	public String toString()
	{
		return "FieldLimit{field=" + field + ", matcher=" + matcher + "}";
	}
}
