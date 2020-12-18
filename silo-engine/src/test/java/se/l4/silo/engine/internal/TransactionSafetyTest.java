package se.l4.silo.engine.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.exobytes.Serializers;
import se.l4.silo.Entity;
import se.l4.silo.Transaction;
import se.l4.silo.engine.EntityCodec;
import se.l4.silo.engine.EntityDefinition;
import se.l4.silo.engine.LocalSilo;

/**
 * Tests related to transaction safety.
 */
public class TransactionSafetyTest
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
	public void testTransactionalMonoCommit()
	{
		TestUserData o = new TestUserData(1, "V1", 20, true);

		instance()
			.transactional(entity().store(o))
			.block();

		assertThat(
			entity().get(1).block(),
			is(o)
		);
	}

	@Test
	public void testTransactionalMonoRollback()
	{
		TestUserData o = new TestUserData(1, "V1", 20, true);

		try
		{
			instance()
				.transactional(
					entity().store(o)
						.then(Mono.error(new IllegalArgumentException("Error")))
				)
				.block();
		}
		catch(IllegalArgumentException e)
		{
			// This is the error expected
		}

		assertThat(
			entity().get(1).block(),
			nullValue()
		);
	}

	@Test
	public void testTransactionalFluxCommit()
	{
		TestUserData[] data = new TestUserData[] {
			new TestUserData(1, "V1", 20, true),
			new TestUserData(2, "V2", 20, true),
			new TestUserData(3, "V3", 20, true)
		};

		instance()
			.transactional(
				Flux.fromArray(data)
					.flatMap(entity()::store)
			)
			.blockLast();

		assertThat(
			entity().get(1).block(),
			is(data[0])
		);
	}

	@Test
	public void testTransactionalFluxRollback()
	{
		TestUserData[] data = new TestUserData[] {
			new TestUserData(1, "V1", 20, true),
			new TestUserData(2, "V2", 20, true),
			new TestUserData(3, "V3", 20, true)
		};

		try
		{
			instance()
				.transactional(
					Flux.fromArray(data)
						.flatMap(o -> {
							if(o.getId() == 3) return Mono.error(new IllegalArgumentException());

							return entity().store(o);
						})
				)
				.blockLast();
		}
		catch(IllegalArgumentException e)
		{
			// This is the error expected
		}

		assertThat(
			entity().get(1).block(),
			nullValue()
		);
	}

	@Test
	public void testTransactionWrapMonoCommit()
	{
		TestUserData o = new TestUserData(1, "V1", 20, true);

		Transaction tx = instance().newTransaction().block();

		tx.wrap(entity().store(o)).block();

		tx.commit().block();

		assertThat(
			entity().get(1).block(),
			is(o)
		);
	}

	@Test
	public void testTransactionWrapMonoRollback()
	{
		TestUserData o = new TestUserData(1, "V1", 20, true);

		Transaction tx = instance().newTransaction().block();

		tx.wrap(entity().store(o)).block();

		tx.rollback().block();

		assertThat(
			entity().get(1).block(),
			nullValue()
		);
	}

	@Test
	public void testTransactionWrapFluxCommit()
	{
		TestUserData[] data = new TestUserData[] {
			new TestUserData(1, "V1", 20, true),
			new TestUserData(2, "V2", 20, true),
			new TestUserData(3, "V3", 20, true)
		};

		Transaction tx = instance().newTransaction().block();

		tx.wrap(
			Flux.fromArray(data)
				.flatMap(entity()::store)
		).blockLast();

		// At this point there should be nothing stored
		assertThat(
			entity().get(1).block(),
			nullValue()
		);

		tx.commit().block();

		assertThat(
			entity().get(1).block(),
			is(data[0])
		);
	}

	@Test
	public void testTransactionWrapFluxRollback()
	{
		TestUserData[] data = new TestUserData[] {
			new TestUserData(1, "V1", 20, true),
			new TestUserData(2, "V2", 20, true),
			new TestUserData(3, "V3", 20, true)
		};

		Transaction tx = instance().newTransaction().block();

		tx.wrap(
			Flux.fromArray(data)
				.flatMap(entity()::store)
		).blockLast();

		tx.rollback().block();

		assertThat(
			entity().get(1).block(),
			nullValue()
		);
	}

	@Test
	public void testTransactionExecuteFluxCommit()
	{
		TestUserData[] data = new TestUserData[] {
			new TestUserData(1, "V1", 20, true),
			new TestUserData(2, "V2", 20, true),
			new TestUserData(3, "V3", 20, true)
		};

		Transaction tx = instance().newTransaction().block();

		tx.execute(tx0 ->
			Flux.fromArray(data)
				.flatMap(entity()::store)
		).blockLast();

		// At this point there should be nothing stored
		assertThat(
			entity().get(1).block(),
			nullValue()
		);

		tx.commit().block();

		assertThat(
			entity().get(1).block(),
			is(data[0])
		);
	}

	@Test
	public void testTransactionExecuteFluxRollback()
	{
		TestUserData[] data = new TestUserData[] {
			new TestUserData(1, "V1", 20, true),
			new TestUserData(2, "V2", 20, true),
			new TestUserData(3, "V3", 20, true)
		};

		Transaction tx = instance().newTransaction().block();

		tx.execute(tx0 ->
			Flux.fromArray(data)
				.flatMap(entity()::store)
		).blockLast();

		tx.rollback().block();

		assertThat(
			entity().get(1).block(),
			nullValue()
		);
	}

	@Test
	public void testWithTransactionWrapFluxCommit()
	{
		TestUserData[] data = new TestUserData[] {
			new TestUserData(1, "V1", 20, true),
			new TestUserData(2, "V2", 20, true),
			new TestUserData(3, "V3", 20, true)
		};

		instance().withTransaction(tx ->
			Flux.fromArray(data)
				.flatMap(entity()::store)
		).blockLast();

		assertThat(
			entity().get(1).block(),
			is(data[0])
		);
	}

	@Test
	public void testWithTransactionWrapFluxRollback()
	{
		TestUserData[] data = new TestUserData[] {
			new TestUserData(1, "V1", 20, true),
			new TestUserData(2, "V2", 20, true),
			new TestUserData(3, "V3", 20, true)
		};

		try
		{
			instance()
				.transactional(
					Flux.fromArray(data)
						.flatMap(o -> {
							if(o.getId() == 3) return Mono.error(new IllegalArgumentException());

							return entity().store(o);
						})
				)
				.blockLast();
		}
		catch(IllegalArgumentException e)
		{
			// This is the error expected
		}

		assertThat(
			entity().get(1).block(),
			nullValue()
		);
	}

	@Test
	public void testInTransactionRunnableCommit()
	{
		TestUserData o = new TestUserData(1, "V1", 20, true);

		instance().inTransaction(() -> {
			entity().store(o).block();
		}).block();

		assertThat(
			entity().get(1).block(),
			is(o)
		);
	}

	@Test
	public void testInTransactionRunnableRollback()
	{
		TestUserData o = new TestUserData(1, "V1", 20, true);

		try
		{
			instance().inTransaction((Runnable) () -> {
				entity().store(o).block();

				throw new IllegalArgumentException();
			}).block();
		}
		catch(IllegalArgumentException e)
		{
			// This is the error expected
		}

		assertThat(
			entity().get(1).block(),
			nullValue()
		);
	}

	@Test
	public void testInTransactionSupplierCommit()
	{
		TestUserData o = new TestUserData(1, "V1", 20, true);

		String value = instance().inTransaction(() -> {
			entity().store(o).block();

			return "test";
		}).block();

		assertThat(value, is("test"));

		assertThat(
			entity().get(1).block(),
			is(o)
		);
	}

	@Test
	public void testInTransactionSupplierRollback()
	{
		TestUserData o = new TestUserData(1, "V1", 20, true);

		try
		{
			instance().inTransaction((Supplier<String>) () -> {
				entity().store(o).block();

				throw new IllegalArgumentException();
			}).block();
		}
		catch(IllegalArgumentException e)
		{
			// This is the error expected
		}

		assertThat(
			entity().get(1).block(),
			nullValue()
		);
	}

	@Test
	public void testTransactionHidesUpdate()
	{
		TestUserData original = new TestUserData(1, "V1", 20, true);
		TestUserData updated = new TestUserData(1, "V2", 20, true);

		// Store the original data
		entity().store(original).block();

		// This transaction should keep old data
		Transaction tx = instance().newTransaction().block();

		// Update the original data, auto-committing it
		entity().store(updated).block();

		// Check that reading data outside TX shows updated data
		assertThat(
			entity().get(1).block(),
			is(updated)
		);

		// Check that reading data inside TX shows original data
		assertThat(
			tx.wrap(entity().get(1)).block(),
			is(original)
		);

		// Release the old transaction
		tx.commit().block();
	}

	@Test
	public void testTransactionHidesDelete()
	{
		TestUserData o = new TestUserData(1, "V1", 20, true);

		// Store the original data
		entity().store(o).block();

		// This transaction should keep old data
		Transaction tx = instance().newTransaction().block();

		// Update the original data, auto-committing it
		entity().delete(1).block();

		// Check that reading data outside TX shows updated data
		assertThat(
			entity().get(1).block(),
			nullValue()
		);

		// Check that reading data inside TX shows original data
		assertThat(
			tx.wrap(entity().get(1)).block(),
			is(o)
		);

		// Release the old transaction
		tx.commit().block();
	}
}
