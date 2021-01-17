package se.l4.silo.engine.index.search;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

import se.l4.silo.Entity;
import se.l4.silo.Transaction;
import se.l4.silo.engine.EntityCodec;
import se.l4.silo.engine.EntityDefinition;
import se.l4.silo.engine.LocalSilo;
import se.l4.silo.engine.internal.BasicTest;
import se.l4.silo.engine.internal.TestUserData;
import se.l4.silo.index.search.PaginatedSearchResult;
import se.l4.silo.index.search.SearchIndexQuery;

public class SearchObjectEntityTest
	extends BasicTest
{
	@Override
	protected LocalSilo.Builder setup(LocalSilo.Builder builder)
	{
		SearchIndexDefinition<TestUserData> index = SearchIndexDefinition.create("index", TestUserData.class)
			.addField(
				SearchFieldDefinition.create("name", TestUserData.class)
					.withType(SearchFields.TEXT)
					.withSupplier(TestUserData::getName)
					.build()
			)
			.addField(
				SearchFieldDefinition.create("age", TestUserData.class)
					.withType(SearchFields.INTEGER)
					.withSupplier(TestUserData::getAge)
					.build()
			)
			.addField(
				SearchFieldDefinition.create("active", TestUserData.class)
					.withType(SearchFields.BOOLEAN)
					.withSupplier(TestUserData::isActive)
					.build()
			)
			.build();

		return builder.addEntity(EntityDefinition.create("test", TestUserData.class)
			.withCodec(EntityCodec.serialized(serializers, TestUserData.class))
			.withId(Integer.class, TestUserData::getId)
			.addIndex(index)
			.build());
	}

	private Entity<Long, TestUserData> entity()
	{
		return instance().entity("test", Long.class, TestUserData.class);
	}

	@Test
	public void testStore()
	{
		Entity<Long, TestUserData> entity = entity();

		TestUserData data = new TestUserData(1, "Donna", 30, true);

		entity.store(data)
			.block();
	}

	@Test
	public void testQueryAll()
	{
		Entity<Long, TestUserData> entity = entity();

		TestUserData data = new TestUserData(1, "Donna", 30, true);

		entity.store(data)
			.block();

		PaginatedSearchResult<TestUserData> result = entity.fetch(
			SearchIndexQuery.create("index", TestUserData.class)
				.limited()
				.build()
		).block();

		assertThat(result.getSize(), is(1l));
	}

	@Test
	public void testQueryInTransaction()
	{
		Entity<Long, TestUserData> entity = entity();

		entity.store(new TestUserData(1, "Donna", 30, true))
			.block();

		Transaction tx = instance().transactions().newTransaction().block();

		entity.store(new TestUserData(2, "Steve", 30, true))
			.block();

		PaginatedSearchResult<TestUserData> result = tx.execute(v ->
			entity.fetch(
				SearchIndexQuery.create("index", TestUserData.class)
					.limited()
					.build()
			)
		).blockLast();

		assertThat(result.getSize(), is(1l));

		tx.commit().block();

		result = entity.fetch(
			SearchIndexQuery.create("index", TestUserData.class)
				.limited()
				.build()
		).block();

		assertThat(result.getSize(), is(2l));
	}
}
