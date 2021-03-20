package se.l4.silo.engine.internal.collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.is;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.junit.jupiter.api.Test;

import se.l4.silo.engine.collection.LongIntervalCollector;

public class SimpleLongIntervalCollectorTest
{
	@Test
	public void testEmpty()
	{
		LongIntervalCollector<String> c = SimpleLongIntervalCollector.<String>create()
			.build();

		MutableList<String> result = Lists.mutable.empty();
		c.match(0, result::add);

		assertThat(result.isEmpty(), is(true));
	}

	@Test
	public void testSingleValue()
	{
		LongIntervalCollector<String> c = SimpleLongIntervalCollector.<String>create()
			.add("a", 10, 20)
			.build();

		MutableList<String> result;

		result = Lists.mutable.empty();
		c.match(9, result::add);
		assertThat(result, emptyIterable());

		result = Lists.mutable.empty();
		c.match(21, result::add);
		assertThat(result, emptyIterable());

		result = Lists.mutable.empty();
		c.match(20, result::add);
		assertThat(result, emptyIterable());

		result = Lists.mutable.empty();
		c.match(19, result::add);
		assertThat(result, contains("a"));

		result = Lists.mutable.empty();
		c.match(10, result::add);
		assertThat(result, contains("a"));

		result = Lists.mutable.empty();
		c.match(11, result::add);
		assertThat(result, contains("a"));
	}

	@Test
	public void testMultipleNonOverlapping()
	{
		LongIntervalCollector<String> c = SimpleLongIntervalCollector.<String>create()
			.add("a", 10, 20)
			.add("b", 20, 30)
			.build();

		MutableList<String> result;

		result = Lists.mutable.empty();
		c.match(9, result::add);
		assertThat(result, emptyIterable());

		result = Lists.mutable.empty();
		c.match(10, result::add);
		assertThat(result, contains("a"));

		result = Lists.mutable.empty();
		c.match(11, result::add);
		assertThat(result, contains("a"));

		result = Lists.mutable.empty();
		c.match(19, result::add);
		assertThat(result, contains("a"));

		result = Lists.mutable.empty();
		c.match(20, result::add);
		assertThat(result, contains("b"));

		result = Lists.mutable.empty();
		c.match(29, result::add);
		assertThat(result, contains("b"));

		result = Lists.mutable.empty();
		c.match(30, result::add);
		assertThat(result, emptyIterable());
	}

	@Test
	public void testMultipleOverlapping()
	{
		LongIntervalCollector<String> c = SimpleLongIntervalCollector.<String>create()
			.add("a", 10, 20)
			.add("b", 19, 30)
			.build();

		MutableList<String> result;

		result = Lists.mutable.empty();
		c.match(9, result::add);
		assertThat(result, emptyIterable());

		result = Lists.mutable.empty();
		c.match(10, result::add);
		assertThat(result, contains("a"));

		result = Lists.mutable.empty();
		c.match(11, result::add);
		assertThat(result, contains("a"));

		result = Lists.mutable.empty();
		c.match(19, result::add);
		assertThat(result, contains("a", "b"));

		result = Lists.mutable.empty();
		c.match(20, result::add);
		assertThat(result, contains("b"));

		result = Lists.mutable.empty();
		c.match(29, result::add);
		assertThat(result, contains("b"));

		result = Lists.mutable.empty();
		c.match(30, result::add);
		assertThat(result, emptyIterable());
	}
}
