package se.l4.silo.engine.internal.index.basic;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import se.l4.silo.Collection;
import se.l4.silo.engine.CollectionDef;
import se.l4.silo.engine.LocalSilo;
import se.l4.silo.engine.ObjectCodec;
import se.l4.silo.engine.index.basic.BasicFieldDef;
import se.l4.silo.engine.index.basic.BasicIndexDef;
import se.l4.silo.engine.internal.SiloTest;
import se.l4.silo.index.basic.BasicIndexQuery;
import se.l4.silo.index.basic.BasicIndexResult;

public class BasicFieldTypesTest
	extends SiloTest
{
	@Test
	public void testLong()
	{
		LocalSilo silo = instance(builder -> builder.addCollection(CollectionDef.create(Long.class, "test")
			.withCodec(ObjectCodec.serialized(serializers, Long.class))
			.withId(Long.class, s -> s)
			.addIndex(BasicIndexDef.create(Long.class, "idx")
				.addField(BasicFieldDef.create(Long.class, "v")
					.withType(long.class)
					.withSupplier(s -> s)
					.build()
				)
				.build()
			)
			.build()
		));

		Collection<Long, Long> collection = silo.getCollection("test", Long.class, Long.class);

		collection.store(0l).block();
		collection.store(100l).block();
		collection.store(200l).block();

		BasicIndexResult<Long> result;

		result = collection.fetch(BasicIndexQuery.create("idx", Long.class)
			.field("v").isLessThan(100)
			.build()
		).block();

		assertThat(result.getSize(), is(1l));

		result = collection.fetch(BasicIndexQuery.create("idx", Long.class)
			.field("v").isLessThanOrEqualTo(100)
			.build()
		).block();

		assertThat(result.getSize(), is(2l));

		result = collection.fetch(BasicIndexQuery.create("idx", Long.class)
			.field("v").isMoreThan(100)
			.build()
		).block();

		assertThat(result.getSize(), is(1l));

		result = collection.fetch(BasicIndexQuery.create("idx", Long.class)
			.field("v").isMoreThanOrEqualTo(100)
			.build()
		).block();

		assertThat(result.getSize(), is(2l));

		result = collection.fetch(BasicIndexQuery.create("idx", Long.class)
			.field("v").isEqualTo(100l)
			.build()
		).block();

		assertThat(result.getSize(), is(1l));
	}

	@Test
	public void testLongLowerLimit()
	{
		LocalSilo silo = instance(builder -> builder.addCollection(CollectionDef.create(Long.class, "test")
			.withCodec(ObjectCodec.serialized(serializers, Long.class))
			.withId(Long.class, s -> s)
			.addIndex(BasicIndexDef.create(Long.class, "idx")
				.addField(BasicFieldDef.create(Long.class, "v")
					.withType(long.class)
					.withSupplier(s -> s)
					.build()
				)
				.build()
			)
			.build()
		));

		Collection<Long, Long> collection = silo.getCollection("test", Long.class, Long.class);

		collection.store(Long.MIN_VALUE).block();

		BasicIndexResult<Long> r1 = collection.fetch(BasicIndexQuery.create("idx", Long.class)
			.field("v").isMoreThan(0)
			.build()
		).block();

		assertThat(r1.getSize(), is(0l));

		BasicIndexResult<Long> r2 = collection.fetch(BasicIndexQuery.create("idx", Long.class)
			.field("v").isMoreThanOrEqualTo(Long.MIN_VALUE)
			.build()
		).block();

		assertThat(r2.getSize(), is(1l));
	}
}
