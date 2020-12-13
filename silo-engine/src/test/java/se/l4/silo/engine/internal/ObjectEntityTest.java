package se.l4.silo.engine.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import se.l4.silo.Entity;
import se.l4.silo.EntityRef;
import se.l4.silo.engine.EntityCodec;
import se.l4.silo.engine.EntityDefinition;
import se.l4.silo.engine.LocalSilo;

public class ObjectEntityTest
	extends BasicTest
{
	@Override
	protected LocalSilo.Builder setup(LocalSilo.Builder builder)
	{
		EntityDefinition<Integer, TestUserData> def = EntityDefinition.create("test", TestUserData.class)
			.withCodec(EntityCodec.serialized(serializers, TestUserData.class))
			.withId(Integer.class, TestUserData::getId)
			.build();

		return builder.addEntity(def);
	}

	private Entity<Integer, TestUserData> entity()
	{
		return instance().entity(EntityRef.create("test", Integer.class, TestUserData.class));
	}

	@Test
	public void testStoreNoTransaction()
	{
		Entity<Integer, TestUserData> entity = entity();

		TestUserData obj = new TestUserData(2, "Donna Johnson", 28, false);
		entity.store(obj).block();

		Optional<TestUserData> fetched = entity.get(2).blockOptional();
		assertThat(fetched.get(), is(obj));
	}

	@Test
	public void testStoreInTransaction()
	{
		Entity<Integer, TestUserData> entity = entity();

		TestUserData obj = new TestUserData(2, "Donna Johnson", 28, false);

		instance().inTransaction(() -> {
			entity.store(obj).block();
		});

		Optional<TestUserData> fetched = entity.get(2).blockOptional();
		assertThat(fetched.get(), is(obj));
	}

	@Test
	public void testStoreDeleteNoTransaction()
	{
		Entity<Integer, TestUserData> entity = entity();

		TestUserData obj = new TestUserData(2, "Donna Johnson", 28, false);
		entity.store(obj).block();
		entity.delete(2).block();

		Optional<TestUserData> fetched = entity.get(2).blockOptional();
		assertThat(fetched.isPresent(), is(false));
	}
}
