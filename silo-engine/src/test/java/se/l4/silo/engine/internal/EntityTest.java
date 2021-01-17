package se.l4.silo.engine.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;
import se.l4.exobytes.Serializers;
import se.l4.silo.Entity;
import se.l4.silo.Transaction;
import se.l4.silo.engine.EntityCodec;
import se.l4.silo.engine.EntityDefinition;
import se.l4.silo.engine.LocalSilo;

public class EntityTest
	extends BasicTest
{
	@Override
	protected LocalSilo.Builder setup(LocalSilo.Builder builder)
	{
		return builder.addEntity(
			EntityDefinition.create("test", TestUserData.class)
				.withId(Integer.class, TestUserData::getId)
				.withCodec(EntityCodec.serialized(Serializers.create().build(), TestUserData.class))
				.build()
		);
	}

	protected Entity<Integer, TestUserData> entity()
	{
		return instance().entity("test", Integer.class, TestUserData.class);
	}

	@Test
	public void storeOutsideTransaction()
	{
		TestUserData o = new TestUserData(1, "V1", 20, true);

		entity().store(o).block();

		TestUserData data = entity().get(1).block();
		assertThat(data, is(o));
	}

	@Test
	public void storeWithManualTransaction()
	{
		TestUserData o = new TestUserData(1, "V1", 20, true);

		Transaction tx = instance().transactions().newTransaction().block();

		// This should store the object but not commit the TX
		tx.execute(ignore -> entity().store(o)).subscribe();

		// No data should be available
		assertThat(
			entity().get(1).blockOptional(),
			is(Optional.empty())
		);

		// Commit the TX
		tx.commit().block();

		// Verify that the data is now available
		assertThat(
			entity().get(1).block(),
			is(o)
		);
	}

	@Test
	public void storeViaTransactional()
	{
		TestUserData o = new TestUserData(1, "V1", 20, true);

		instance()
			.transactions()
			.transactional(entity().store(o))
			.block();

		assertThat(
			entity().get(1).block(),
			is(o)
		);
	}

	@Test
	public void storeAndDelete()
	{
		TestUserData o = new TestUserData(1, "V1", 20, true);

		entity().store(o).block();

		TestUserData data = entity().get(1).block();
		assertThat(data, is(o));

		entity().delete(1).block();

		assertThat(
			entity().get(1).blockOptional(),
			is(Optional.empty())
		);
	}

	@Test
	public void storeAndOverwrite()
	{
		TestUserData o1 = new TestUserData(1, "V1", 20, true);

		entity().store(o1).block();

		TestUserData d1 = entity().get(1).block();
		assertThat(d1, is(o1));

		TestUserData o2 = new TestUserData(1, "V2", 20, true);

		entity().store(o2).block();

		TestUserData d2 = entity().get(1).block();
		assertThat(d2, is(o2));
	}

	@Test
	public void overwriteInTransaction()
	{
		TestUserData o1 = new TestUserData(1, "V1", 20, true);
		TestUserData o2 = new TestUserData(1, "V2", 20, true);

		instance().transactions().inTransaction(() -> {
			entity().store(o1).block();
			entity().store(o2).block();
		}).block();

		TestUserData d2 = entity().get(1).block();
		assertThat(d2, is(o2));
	}

	@Test
	public void storeFluxNoTransaction()
	{
		Flux.range(1, 1000)
			.map(i -> new TestUserData(i, "V" + i, i % 40, i % 2 == 0))
			.flatMap(entity()::store)
			.blockLast();

		assertThat(
			entity().get(1).block(),
			notNullValue()
		);

		assertThat(
			entity().get(1000).block(),
			notNullValue()
		);
	}

	@Test
	public void storeFluxTransactional()
	{
		instance().transactions().transactional(
			Flux.range(1, 1000)
				.map(i -> new TestUserData(i, "V" + i, i % 40, i % 2 == 0))
				.flatMap(entity()::store)
		).blockLast();

		assertThat(
			entity().get(1).block(),
			notNullValue()
		);

		assertThat(
			entity().get(1000).block(),
			notNullValue()
		);
	}
}
