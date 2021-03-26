package se.l4.silo.engine.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;
import se.l4.exobytes.Serializers;
import se.l4.silo.Collection;
import se.l4.silo.Transaction;
import se.l4.silo.engine.CollectionDef;
import se.l4.silo.engine.LocalSilo;
import se.l4.silo.engine.ObjectCodec;

public class CollectionTest
	extends BasicTest
{
	@Override
	protected LocalSilo.Builder setup(LocalSilo.Builder builder)
	{
		return builder.addCollection(
			CollectionDef.create(TestUserData.class, "test")
				.withId(Integer.class, TestUserData::getId)
				.withCodec(ObjectCodec.serialized(Serializers.create().build(), TestUserData.class))
		);
	}

	protected Collection<Integer, TestUserData> collection()
	{
		return instance().getCollection("test", Integer.class, TestUserData.class);
	}

	@Test
	public void storeOutsideTransaction()
	{
		TestUserData o = new TestUserData(1, "V1", 20, true);

		collection().store(o).block();

		TestUserData data = collection().get(1).block();
		assertThat(data, is(o));
	}

	@Test
	public void storeWithManualTransaction()
	{
		TestUserData o = new TestUserData(1, "V1", 20, true);

		Transaction tx = instance().transactions().newTransaction().block();

		// This should store the object but not commit the TX
		tx.execute(ignore -> collection().store(o)).subscribe();

		// No data should be available
		assertThat(
			collection().get(1).blockOptional(),
			is(Optional.empty())
		);

		// Commit the TX
		tx.commit().block();

		// Verify that the data is now available
		assertThat(
			collection().get(1).block(),
			is(o)
		);
	}

	@Test
	public void storeViaTransactional()
	{
		TestUserData o = new TestUserData(1, "V1", 20, true);

		instance()
			.transactions()
			.transactional(collection().store(o))
			.block();

		assertThat(
			collection().get(1).block(),
			is(o)
		);
	}

	@Test
	public void storeAndDelete()
	{
		TestUserData o = new TestUserData(1, "V1", 20, true);

		collection().store(o).block();

		TestUserData data = collection().get(1).block();
		assertThat(data, is(o));

		collection().delete(1).block();

		assertThat(
			collection().get(1).blockOptional(),
			is(Optional.empty())
		);
	}

	@Test
	public void storeAndOverwrite()
	{
		TestUserData o1 = new TestUserData(1, "V1", 20, true);

		collection().store(o1).block();

		TestUserData d1 = collection().get(1).block();
		assertThat(d1, is(o1));

		TestUserData o2 = new TestUserData(1, "V2", 20, true);

		collection().store(o2).block();

		TestUserData d2 = collection().get(1).block();
		assertThat(d2, is(o2));
	}

	@Test
	public void overwriteInTransaction()
	{
		TestUserData o1 = new TestUserData(1, "V1", 20, true);
		TestUserData o2 = new TestUserData(1, "V2", 20, true);

		instance().transactions().inTransaction(() -> {
			collection().store(o1).block();
			collection().store(o2).block();
		}).block();

		TestUserData d2 = collection().get(1).block();
		assertThat(d2, is(o2));
	}

	@Test
	public void storeFluxNoTransaction()
	{
		Flux.range(1, 1000)
			.map(i -> new TestUserData(i, "V" + i, i % 40, i % 2 == 0))
			.flatMap(collection()::store)
			.blockLast();

		assertThat(
			collection().get(1).block(),
			notNullValue()
		);

		assertThat(
			collection().get(1000).block(),
			notNullValue()
		);
	}

	@Test
	public void storeFluxTransactional()
	{
		instance().transactions().transactional(
			Flux.range(1, 1000)
				.map(i -> new TestUserData(i, "V" + i, i % 40, i % 2 == 0))
				.flatMap(collection()::store)
		).blockLast();

		assertThat(
			collection().get(1).block(),
			notNullValue()
		);

		assertThat(
			collection().get(1000).block(),
			notNullValue()
		);
	}
}
