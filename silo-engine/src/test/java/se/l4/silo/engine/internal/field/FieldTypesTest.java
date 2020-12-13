package se.l4.silo.engine.internal.field;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

import se.l4.silo.Entity;
import se.l4.silo.engine.EntityCodec;
import se.l4.silo.engine.EntityDefinition;
import se.l4.silo.engine.LocalSilo;
import se.l4.silo.engine.index.FieldDefinition;
import se.l4.silo.engine.index.FieldIndexDefinition;
import se.l4.silo.engine.internal.SiloTest;
import se.l4.silo.index.FieldIndexQuery;
import se.l4.silo.index.FieldIndexResult;

public class FieldTypesTest
	extends SiloTest
{
	@Test
	public void testLong()
	{
		LocalSilo silo = instance(builder -> builder.addEntity(EntityDefinition.create("test", Long.class)
			.withCodec(EntityCodec.serialized(serializers, Long.class))
			.withId(Long.class, s -> s)
			.addIndex(FieldIndexDefinition.create("idx", Long.class)
				.addField(FieldDefinition.create("v", long.class)
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

		FieldIndexResult<Long> result;

		result = entity.fetch(FieldIndexQuery.create("idx", Long.class)
			.field("v").isLessThan(100)
			.build()
		).block();

		assertThat(result.getSize(), is(1l));

		result = entity.fetch(FieldIndexQuery.create("idx", Long.class)
			.field("v").isLessThanOrEqualTo(100)
			.build()
		).block();

		assertThat(result.getSize(), is(2l));

		result = entity.fetch(FieldIndexQuery.create("idx", Long.class)
			.field("v").isMoreThan(100)
			.build()
		).block();

		assertThat(result.getSize(), is(1l));

		result = entity.fetch(FieldIndexQuery.create("idx", Long.class)
			.field("v").isMoreThanOrEqualTo(100)
			.build()
		).block();

		assertThat(result.getSize(), is(2l));

		result = entity.fetch(FieldIndexQuery.create("idx", Long.class)
			.field("v").isEqualTo(100l)
			.build()
		).block();

		assertThat(result.getSize(), is(1l));
	}

	@Test
	public void testLongLowerLimit()
	{
		LocalSilo silo = instance(builder -> builder.addEntity(EntityDefinition.create("test", Long.class)
			.withCodec(EntityCodec.serialized(serializers, Long.class))
			.withId(Long.class, s -> s)
			.addIndex(FieldIndexDefinition.create("idx", Long.class)
				.addField(FieldDefinition.create("v", long.class)
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

		FieldIndexResult<Long> r1 = entity.fetch(FieldIndexQuery.create("idx", Long.class)
			.field("v").isMoreThan(0)
			.build()
		).block();

		assertThat(r1.getSize(), is(0l));

		FieldIndexResult<Long> r2 = entity.fetch(FieldIndexQuery.create("idx", Long.class)
			.field("v").isMoreThanOrEqualTo(Long.MIN_VALUE)
			.build()
		).block();

		assertThat(r2.getSize(), is(1l));
	}
}
