package se.l4.silo.engine.internal.index.basic;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

import se.l4.silo.Entity;
import se.l4.silo.engine.EntityCodec;
import se.l4.silo.engine.EntityDefinition;
import se.l4.silo.engine.LocalSilo;
import se.l4.silo.engine.index.basic.BasicFieldDefinition;
import se.l4.silo.engine.index.basic.BasicIndexDefinition;
import se.l4.silo.engine.internal.SiloTest;
import se.l4.silo.index.basic.BasicIndexQuery;
import se.l4.silo.index.basic.BasicIndexResult;

public class BasicFieldTypesTest
	extends SiloTest
{
	@Test
	public void testLong()
	{
		LocalSilo silo = instance(builder -> builder.addEntity(EntityDefinition.create(Long.class, "test")
			.withCodec(EntityCodec.serialized(serializers, Long.class))
			.withId(Long.class, s -> s)
			.addIndex(BasicIndexDefinition.create(Long.class, "idx")
				.addField(BasicFieldDefinition.create(Long.class, "v")
					.withType(long.class)
					.withSupplier(s -> s)
					.build()
				)
				.build()
			)
			.build()
		));

		Entity<Long, Long> entity = silo.entity("test", Long.class, Long.class);

		entity.store(0l).block();
		entity.store(100l).block();
		entity.store(200l).block();

		BasicIndexResult<Long> result;

		result = entity.fetch(BasicIndexQuery.create("idx", Long.class)
			.field("v").isLessThan(100)
			.build()
		).block();

		assertThat(result.getSize(), is(1l));

		result = entity.fetch(BasicIndexQuery.create("idx", Long.class)
			.field("v").isLessThanOrEqualTo(100)
			.build()
		).block();

		assertThat(result.getSize(), is(2l));

		result = entity.fetch(BasicIndexQuery.create("idx", Long.class)
			.field("v").isMoreThan(100)
			.build()
		).block();

		assertThat(result.getSize(), is(1l));

		result = entity.fetch(BasicIndexQuery.create("idx", Long.class)
			.field("v").isMoreThanOrEqualTo(100)
			.build()
		).block();

		assertThat(result.getSize(), is(2l));

		result = entity.fetch(BasicIndexQuery.create("idx", Long.class)
			.field("v").isEqualTo(100l)
			.build()
		).block();

		assertThat(result.getSize(), is(1l));
	}

	@Test
	public void testLongLowerLimit()
	{
		LocalSilo silo = instance(builder -> builder.addEntity(EntityDefinition.create(Long.class, "test")
			.withCodec(EntityCodec.serialized(serializers, Long.class))
			.withId(Long.class, s -> s)
			.addIndex(BasicIndexDefinition.create(Long.class, "idx")
				.addField(BasicFieldDefinition.create(Long.class, "v")
					.withType(long.class)
					.withSupplier(s -> s)
					.build()
				)
				.build()
			)
			.build()
		));

		Entity<Long, Long> entity = silo.entity("test", Long.class, Long.class);

		entity.store(Long.MIN_VALUE).block();

		BasicIndexResult<Long> r1 = entity.fetch(BasicIndexQuery.create("idx", Long.class)
			.field("v").isMoreThan(0)
			.build()
		).block();

		assertThat(r1.getSize(), is(0l));

		BasicIndexResult<Long> r2 = entity.fetch(BasicIndexQuery.create("idx", Long.class)
			.field("v").isMoreThanOrEqualTo(Long.MIN_VALUE)
			.build()
		).block();

		assertThat(r2.getSize(), is(1l));
	}
}
