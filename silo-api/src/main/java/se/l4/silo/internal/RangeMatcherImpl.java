package se.l4.silo.internal;

import java.util.Objects;
import java.util.Optional;

import se.l4.silo.index.RangeMatcher;

/**
 * Implementation of {@link RangeMatcher}.
 */
public class RangeMatcherImpl<T>
	implements RangeMatcher<T>
{
	private final T lower;
	private final boolean lowerInclusive;
	private final T upper;
	private final boolean upperInclusive;

	public RangeMatcherImpl(
		T lower,
		boolean lowerInclusive,
		T upper,
		boolean upperInclusive
	)
	{
		this.lower = lower;
		this.lowerInclusive = lowerInclusive;
		this.upper = upper;
		this.upperInclusive = upperInclusive;
	}

	@Override
	public Optional<T> getLower()
	{
		return Optional.ofNullable(lower);
	}

	@Override
	public boolean isLowerInclusive()
	{
		return lowerInclusive;
	}

	@Override
	public Optional<T> getUpper()
	{
		return Optional.ofNullable(upper);
	}

	@Override
	public boolean isUpperInclusive()
	{
		return upperInclusive;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(lower, lowerInclusive, upper, upperInclusive);
	}

	@Override
	public boolean equals(Object obj)
	{
		if(this == obj) return true;
		if(obj == null) return false;
		if(getClass() != obj.getClass()) return false;
		RangeMatcherImpl other = (RangeMatcherImpl) obj;
		return Objects.equals(lower, other.lower)
			&& lowerInclusive == other.lowerInclusive
			&& Objects.equals(upper, other.upper)
			&& upperInclusive == other.upperInclusive;
	}

	@Override
	public String toString()
	{
		return "RangeMatcher{lower=" + lower + ", lowerInclusive="
			+ lowerInclusive + ", upper=" + upper + ", upperInclusive="
			+ upperInclusive + "}";
	}
}
