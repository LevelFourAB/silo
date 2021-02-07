package se.l4.silo.engine.internal.collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import org.junit.jupiter.api.Test;

public class MapBackedTopKTest
{
	@Test
	public void testOfferUniqueCounts()
	{
		MapBackedTopK<String> o = new MapBackedTopK<>(5);
		o.offer("a", 1);
		o.offer("b", 2);
		o.offer("c", 3);
		o.offer("d", 4);
		o.offer("e", 5);
		o.offer("f", 6);

		assertThat(o.items(), contains("f", "e", "d", "c", "b"));
	}

	@Test
	public void testOfferSameCounts()
	{
		MapBackedTopK<String> o = new MapBackedTopK<>(5);
		o.offer("a", 1);
		o.offer("b", 2);
		o.offer("c", 3);
		o.offer("d", 3);
		o.offer("e", 4);
		o.offer("f", 5);

		assertThat(o.items(), contains("f", "e", "d", "c", "b"));
	}
}
