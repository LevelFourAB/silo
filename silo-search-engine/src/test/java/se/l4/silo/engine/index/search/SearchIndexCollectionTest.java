package se.l4.silo.engine.index.search;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import se.l4.silo.Collection;
import se.l4.silo.Transaction;
import se.l4.silo.engine.CollectionDef;
import se.l4.silo.engine.LocalSilo;
import se.l4.silo.engine.ObjectCodec;
import se.l4.silo.engine.index.search.types.SearchFieldType;
import se.l4.silo.engine.internal.BasicTest;
import se.l4.silo.engine.internal.TestUserData;
import se.l4.silo.index.search.PaginatedSearchResult;
import se.l4.silo.index.search.SearchIndexQuery;

public class SearchIndexCollectionTest
	extends BasicTest
{
	@Override
	protected LocalSilo.Builder setup(LocalSilo.Builder builder)
	{
		SearchIndexDefinition<TestUserData> index = SearchIndexDefinition.create(TestUserData.class, "index")
			.addField(
				SearchFieldDefinition.create(TestUserData.class, "name")
					.withType(SearchFieldType.forString().fullText().build())
					.withSupplier(TestUserData::getName)
					.build()
			)
			.addField(
				SearchFieldDefinition.create(TestUserData.class, "age")
					.withType(SearchFieldType.forInteger().build())
					.withSupplier(TestUserData::getAge)
					.build()
			)
			.addField(
				SearchFieldDefinition.create(TestUserData.class, "active")
					.withType(SearchFieldType.forBoolean().build())
					.withSupplier(TestUserData::isActive)
					.build()
			)
			.build();

		return builder.addCollection(CollectionDef.create(TestUserData.class, "test")
			.withCodec(ObjectCodec.serialized(serializers, TestUserData.class))
			.withId(Integer.class, TestUserData::getId)
			.addIndex(index)
			.build());
	}

	private Collection<Long, TestUserData> collection()
	{
		return instance().getCollection("test", Long.class, TestUserData.class);
	}

	@Test
	public void testStore()
	{
		Collection<Long, TestUserData> collection = collection();

		TestUserData data = new TestUserData(1, "Donna", 30, true);

		collection.store(data)
			.block();
	}

	@Test
	public void testQueryAll()
	{
		Collection<Long, TestUserData> collection = collection();

		TestUserData data = new TestUserData(1, "Donna", 30, true);

		collection.store(data)
			.block();

		PaginatedSearchResult<TestUserData> result = collection.fetch(
			SearchIndexQuery.create("index", TestUserData.class)
				.limited()
				.build()
		).block();

		assertThat(result.getSize(), is(1l));
	}

	@Test
	public void testQueryInTransaction()
	{
		Collection<Long, TestUserData> collection = collection();

		collection.store(new TestUserData(1, "Donna", 30, true))
			.block();

		Transaction tx = instance().transactions().newTransaction().block();

		collection.store(new TestUserData(2, "Steve", 30, true))
			.block();

		PaginatedSearchResult<TestUserData> result = tx.execute(v ->
			collection.fetch(
				SearchIndexQuery.create("index", TestUserData.class)
					.limited()
					.build()
			)
		).blockLast();

		assertThat(result.getSize(), is(1l));

		tx.commit().block();

		result = collection.fetch(
			SearchIndexQuery.create("index", TestUserData.class)
				.limited()
				.build()
		).block();

		assertThat(result.getSize(), is(2l));
	}
}
