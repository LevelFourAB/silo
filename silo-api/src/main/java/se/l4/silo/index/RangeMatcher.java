package se.l4.silo.index;

import java.util.Optional;

import se.l4.silo.internal.RangeMatcherImpl;

/**
 * {@link Matcher} used to match a continuos interval where each side can be
 * either open or closed.
 */
public interface RangeMatcher<T>
	extends Matcher
{
	/**
	 * Get the lower limit.
	 *
	 * @return
	 */
	Optional<T> getLower();

	/**
	 * Get if the lower limit is inclusive.
	 *
	 * @return
	 */
	boolean isLowerInclusive();

	/**
	 * Get the upper limit.
	 *
	 * @return
	 */
	Optional<T> getUpper();

	/**
	 * Get if the upper limit is inclusive.
	 *
	 * @return
	 */
	boolean isUpperInclusive();

	/**
	 * Get a matcher that will match anything that is less than the given
	 * value.
	 *
	 * @param <T>
	 * @param value
	 * @return
	 *   interval representing {@code [MIN, value)}
	 */
	static <T> RangeMatcher<T> isLessThan(
		T value
	)
	{
		return new RangeMatcherImpl<>(null, false, value, false);
	}

	/**
	 * Get a matcher that will match anything that is less than or equal to
	 * the given value.
	 *
	 * @param <T>
	 * @param value
	 * @return
	 *   interval representing {@code [MIN, value]}
	 */
	static <T> RangeMatcher<T> isLessThanOrEqualTo(
		T value
	)
	{
		return new RangeMatcherImpl<>(null, false, value, true);
	}

	/**
	 * Get a matcher that will match anything that is more than the given
	 * value.
	 *
	 * @param <T>
	 * @param value
	 * @return
	 *   interval representing {@code (value, MAX]}
	 */
	static <T> RangeMatcher<T> isMoreThan(
		T value
	)
	{
		return new RangeMatcherImpl<>(value, false, null, false);
	}

	/**
	 * Get a matcher that will match anything that is more than or equal to
	 * the given value.
	 *
	 * @param <T>
	 * @param value
	 * @return
	 *   interval representing {@code [value, MAX]}
	 */
	static <T> RangeMatcher<T> isMoreThanOrEqualTo(
		T value
	)
	{
		return new RangeMatcherImpl<>(value, true, null, false);
	}

	/**
	 * Get a matcher representing a range, with lower and upper being
	 * inclusive or not.
	 *
	 * @param <T>
	 * @param lower
	 * @param lowerInclusive
	 * @param upper
	 * @param upperInclusive
	 * @return
	 *   interval
	 */
	static <T> RangeMatcher<T> between(
		T lower,
		boolean lowerInclusive,
		T upper,
		boolean upperInclusive
	)
	{
		return new RangeMatcherImpl<>(lower, lowerInclusive, upper, upperInclusive);
	}

	/**
	 * Get matcher representing a range where both the lower and upper limit
	 * are inclusive.
	 *
	 * @param <T>
	 * @param lower
	 * @param upper
	 * @return
	 *   interval representing {@code [lower, upper]}
	 */
	static <T> RangeMatcher<T> betweenInclusive(
		T lower,
		T upper
	)
	{
		return new RangeMatcherImpl<>(lower, true, upper, true);
	}

	/**
	 * Get matcher representing a range where both the lower and upper limit
	 * are exclusive.
	 *
	 * @param <T>
	 * @param lower
	 * @param upper
	 * @return
	 *   interval representing {@code (lower, upper)}
	 */
	static <T> RangeMatcher<T> betweenExclusive(
		T lower,
		T upper
	)
	{
		return new RangeMatcherImpl<>(lower, false, upper, false);
	}

	/**
	 * Get a matcher representing a range where the lower limit is exclusive
	 * and the upper is inclusive.
	 *
	 * @param <T>
	 * @param lower
	 * @param upper
	 * @return
	 *   interval representing {@code (lower, upper]}
	 */
	static <T> RangeMatcher<T> betweenLowerExclusive(
		T lower,
		T upper
	)
	{
		return new RangeMatcherImpl<>(lower, false, upper, true);
	}

	/**
	 * Get a matcher representing a range where the lower limit is inclusive
	 * and the upper is exclusive.
	 *
	 * @param <T>
	 * @param lower
	 * @param upper
	 * @return
	 *   interval representing {@code [lower, upper)}
	 */
	static <T> RangeMatcher<T> betweenUpperExclusive(
		T lower,
		T upper
	)
	{
		return new RangeMatcherImpl<>(lower, true, upper, false);
	}

	interface ComposableBuilder<ReturnPath, V>
		extends Matcher.ComposableBuilder<ReturnPath>
	{
		/**
		 * Get a matcher that will match anything that is less than the given
		 * value.
		 *
		 * @param value
		 * @return
		 *   interval representing {@code [MIN, value)}
		 */
		default ReturnPath isLessThan(
			V value
		)
		{
			return matcher(RangeMatcher.isLessThan(value));
		}

		/**
		 * Get a matcher that will match anything that is less than or equal to
		 * the given value.
		 *
		 * @param value
		 * @return
		 *   interval representing {@code [MIN, value]}
		 */
		default ReturnPath isLessThanOrEqualTo(
			V value
		)
		{
			return matcher(RangeMatcher.isLessThanOrEqualTo(value));
		}

		/**
		 * Get a matcher that will match anything that is more than the given
		 * value.
		 *
		 * @param value
		 * @return
		 *   interval representing {@code (value, MAX]}
		 */
		default ReturnPath isMoreThan(
			V value
		)
		{
			return matcher(RangeMatcher.isMoreThan(value));
		}

		/**
		 * Get a matcher that will match anything that is more than or equal to
		 * the given value.
		 *
		 * @param value
		 * @return
		 *   interval representing {@code [value, MAX]}
		 */
		default ReturnPath isMoreThanOrEqualTo(
			V value
		)
		{
			return matcher(RangeMatcher.isMoreThanOrEqualTo(value));
		}

		/**
		 * Get a matcher representing a range, with lower and upper being
		 * inclusive or not.
		 *
		 * @param lower
		 * @param lowerInclusive
		 * @param upper
		 * @param upperInclusive
		 * @return
		 *   interval
		 */
		default ReturnPath between(
			V lower,
			boolean lowerInclusive,
			V upper,
			boolean upperInclusive
		)
		{
			return matcher(RangeMatcher.between(lower, lowerInclusive, upper, upperInclusive));
		}

		/**
		 * Get matcher representing a range where both the lower and upper limit
		 * are inclusive.
		 *
		 * @param lower
		 * @param upper
		 * @return
		 *   interval representing {@code [lower, upper]}
		 */
		default ReturnPath betweenInclusive(
			V lower,
			V upper
		)
		{
			return matcher(RangeMatcher.betweenInclusive(lower, upper));
		}

		/**
		 * Get matcher representing a range where both the lower and upper limit
		 * are exclusive.
		 *
		 * @param lower
		 * @param upper
		 * @return
		 *   interval representing {@code (lower, upper)}
		 */
		default ReturnPath betweenExclusive(
			V lower,
			V upper
		)
		{
			return matcher(RangeMatcher.betweenExclusive(lower, upper));
		}

		/**
		 * Get a matcher representing a range where the lower limit is exclusive
		 * and the upper is inclusive.
		 *
		 * @param lower
		 * @param upper
		 * @return
		 *   interval representing {@code (lower, upper]}
		 */
		default ReturnPath betweenLowerExclusive(
			V lower,
			V upper
		)
		{
			return matcher(RangeMatcher.betweenLowerExclusive(lower, upper));
		}

		/**
		 * Get a matcher representing a range where the lower limit is inclusive
		 * and the upper is exclusive.
		 *
		 * @param lower
		 * @param upper
		 * @return
		 *   interval representing {@code [lower, upper)}
		 */
		default ReturnPath betweenUpperExclusive(
			V lower,
			V upper
		)
		{
			return matcher(RangeMatcher.betweenUpperExclusive(lower, upper));
		}
	}
}
