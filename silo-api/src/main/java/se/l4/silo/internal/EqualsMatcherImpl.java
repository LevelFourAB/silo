package se.l4.silo.internal;

import java.util.Objects;

import se.l4.silo.index.EqualsMatcher;

/**
 * Implementation of {@link EqualsMatcher}.
 */
public class EqualsMatcherImpl
	implements EqualsMatcher
{
	private final Object value;

	public EqualsMatcherImpl(
		Object value
	)
	{
		this.value = value;
	}

	@Override
	public Object getValue()
	{
		return value;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(value);
	}

	@Override
	public boolean equals(Object obj)
	{
		if(this == obj) return true;
		if(obj == null) return false;
		if(getClass() != obj.getClass()) return false;
		EqualsMatcherImpl other = (EqualsMatcherImpl) obj;
		return Objects.equals(value, other.value);
	}

	@Override
	public String toString()
	{
		return "EqualsMatcher{value=" + value + "}";
	}
}
