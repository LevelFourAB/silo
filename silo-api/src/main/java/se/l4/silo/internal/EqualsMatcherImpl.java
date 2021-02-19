package se.l4.silo.internal;

import java.util.Objects;
import java.util.function.Function;

import se.l4.silo.index.EqualsMatcher;
import se.l4.silo.index.Matcher;

/**
 * Implementation of {@link EqualsMatcher}.
 */
public class EqualsMatcherImpl<V>
	implements EqualsMatcher<V>
{
	private final V value;

	public EqualsMatcherImpl(
		V value
	)
	{
		this.value = value;
	}

	@Override
	public V getValue()
	{
		return value;
	}

	@Override
	public <NV> Matcher<NV> map(Function<V, NV> func)
	{
		return new EqualsMatcherImpl<>(func.apply(value));
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
