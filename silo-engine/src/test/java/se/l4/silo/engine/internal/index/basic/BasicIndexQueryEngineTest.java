package se.l4.silo.engine.internal.index.basic;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import se.l4.exobytes.AnnotationSerialization;
import se.l4.exobytes.Expose;
import se.l4.silo.Entity;
import se.l4.silo.EntityRef;
import se.l4.silo.engine.EntityCodec;
import se.l4.silo.engine.EntityDefinition;
import se.l4.silo.engine.LocalSilo;
import se.l4.silo.engine.index.basic.BasicFieldDefinition;
import se.l4.silo.engine.index.basic.BasicIndexDefinition;
import se.l4.silo.engine.internal.BasicTest;
import se.l4.silo.index.basic.BasicIndexQuery;
import se.l4.silo.index.basic.BasicIndexResult;

public class BasicIndexQueryEngineTest
	extends BasicTest
{
	@Override
	protected LocalSilo.Builder setup(LocalSilo.Builder builder)
	{
		BasicFieldDefinition<TestData, String> field1 = BasicFieldDefinition.create(TestData.class, "field1")
			.withType(String.class)
			.withSupplier(TestData::getField1)
			.build();

		BasicFieldDefinition<TestData, Boolean> field2 = BasicFieldDefinition.create(TestData.class, "field2")
			.withType(boolean.class)
			.withSupplier(TestData::isField2)
			.build();

		BasicFieldDefinition<TestData, String> field3 = BasicFieldDefinition.create(TestData.class, "field3")
			.withType(String.class)
			.collection()
			.withSupplier(TestData::getField3)
			.build();

		EntityDefinition<Long, TestData> test = EntityDefinition.create(TestData.class, "test")
			.withCodec(EntityCodec.serialized(serializers, TestData.class))
			.withId(Long.class, TestData::getId)
			.addIndex(BasicIndexDefinition.create(TestData.class, "byField1")
				.addField(field1)
				.build()
			)
			.addIndex(BasicIndexDefinition.create(TestData.class, "byField2")
				.addField(field2)
				.build()
			)
			.addIndex(BasicIndexDefinition.create(TestData.class, "byField3")
				.addField(field3)
				.build()
			)
			.addIndex(BasicIndexDefinition.create(TestData.class, "multiple")
				.addField(field2)
				.addField(field1)
				.build()
			)
			.build();

		return builder.addEntity(test);
	}

	private Entity<Long, TestData> entity()
	{
		return instance().entity(EntityRef.create("test", Long.class, TestData.class));
	}

	@Test
	public void testStore()
	{
		Entity<Long, TestData> entity = entity();

		TestData obj = new TestData(1, "value1", false, Collections.emptyList());
		entity.store(obj).block();

		BasicIndexResult<TestData> fr = entity.fetch(BasicIndexQuery.create("byField1", TestData.class)
			.field("field1").isEqualTo("value1")
			.build()
		).block();

		assertThat(fr.getSize(), is(1l));
		assertThat(fr.first().block(), is(obj));
	}

	@Test
	public void testStoreDelete()
	{
		Entity<Long, TestData> entity = entity();

		TestData obj = new TestData(1, "value1", false, Collections.emptyList());
		entity.store(obj).block();

		BasicIndexResult<TestData> fr = entity.fetch(BasicIndexQuery.create("byField1", TestData.class)
			.field("field1").isEqualTo("value1")
			.build()
		).block();

		assertThat(fr.getSize(), is(1l));

		entity.delete(1l).block();

		fr = entity.fetch(BasicIndexQuery.create("byField1", TestData.class)
			.field("field1").isEqualTo("value1")
			.build()
		).block();

		assertThat(fr.getSize(), is(0l));
	}

	@Test
	public void testStoreReplace()
	{
		Entity<Long, TestData> entity = entity();

		TestData obj = new TestData(1, "value1", false, Collections.emptyList());
		entity.store(obj).block();

		BasicIndexResult<TestData> fr = entity.fetch(BasicIndexQuery.create("byField1", TestData.class)
			.field("field1").isEqualTo("value1")
			.build()
		).block();

		assertThat(fr.getSize(), is(1l));

		// Replace with new data
		obj = new TestData(1, "value2", false, Collections.emptyList());
		entity.store(obj).block();

		// value1 should no longer match
		fr = entity.fetch(BasicIndexQuery.create("byField1", TestData.class)
			.field("field1").isEqualTo("value1")
			.build()
		).block();

		assertThat(fr.getSize(), is(0l));

		// value2 should now match
		fr = entity.fetch(BasicIndexQuery.create("byField1", TestData.class)
			.field("field1").isEqualTo("value2")
			.build()
		).block();

		assertThat(fr.getSize(), is(1l));
	}

	@Test
	public void testStoreMultiple1()
	{
		Entity<Long, TestData> entity = entity();

		TestData obj1 = new TestData(1, "value1", false, Collections.emptyList());
		entity.store(obj1).block();
		TestData obj2 = new TestData(2, "value1", true, Collections.emptyList());
		entity.store(obj2).block();

		BasicIndexResult<TestData> fr = entity.fetch(BasicIndexQuery.create("byField1", TestData.class)
			.field("field1").isEqualTo("value1")
			.build()
		).block();

		assertThat(fr.getSize(), is(2l));
	}

	@Test
	public void testStoreMultiple2()
	{
		Entity<Long, TestData> entity = entity();

		instance().transactions().inTransaction(() -> {
			TestData obj1 = new TestData(1, "value2", false, Collections.emptyList());
			entity.store(obj1).block();
			TestData obj2 = new TestData(2, "value2", true, Collections.emptyList());
			entity.store(obj2).block();
		}).block();

		BasicIndexResult<TestData> fr = entity.fetch(BasicIndexQuery.create("byField1", TestData.class)
			.field("field1").isEqualTo("value2")
			.build()
		).block();

		assertThat(fr.getSize(), is(2l));
	}

	@Test
	public void testStoreDeleteStore()
	{
		Entity<Long, TestData> entity = entity();

		TestData obj1 = new TestData(1, "value1", false, Collections.emptyList());
		entity.store(obj1).block();

		BasicIndexResult<TestData> fr = entity.fetch(BasicIndexQuery.create("byField1", TestData.class)
			.field("field1").isEqualTo("value1")
			.build()
		).block();

		assertThat(fr.getSize(), is(1l));

		TestData obj2 = new TestData(1, "value2", false, Collections.emptyList());
		entity.store(obj2).block();

		fr = entity.fetch(BasicIndexQuery.create("byField1", TestData.class)
			.field("field1").isEqualTo("value1")
			.build()
		).block();

		assertThat(fr.getSize(), is(0l));

		TestData obj3 = new TestData(1, "value1", false, Collections.emptyList());
		entity.store(obj3).block();

		fr = entity.fetch(BasicIndexQuery.create("byField1", TestData.class)
			.field("field1").isEqualTo("value1")
			.build()
		).block();

		assertThat(fr.getSize(), is(1l));
	}

	@Test
	public void testCollection()
	{
		Entity<Long, TestData> entity = entity();

		TestData obj1 = new TestData(1, "value1", false, List.of("v2", "v1"));
		entity.store(obj1).block();

		BasicIndexResult<TestData> fr = entity.fetch(BasicIndexQuery.create("byField3", TestData.class)
			.field("field3").isEqualTo("v1")
			.build()
		).block();

		assertThat(fr.getSize(), is(1l));
	}

	@AnnotationSerialization
	public static class TestData
	{
		@Expose
		private final long id;
		@Expose
		private final String field1;
		@Expose
		private final boolean field2;
		@Expose
		private final List<String> field3;

		public TestData(
			@Expose("id") long id,
			@Expose("field1") String field1,
			@Expose("field2") boolean field2,
			@Expose("field3") List<String> field3
		)
		{
			this.id = id;
			this.field1 = field1;
			this.field2 = field2;
			this.field3 = field3;
		}

		public long getId()
		{
			return id;
		}

		public String getField1()
		{
			return field1;
		}

		public boolean isField2()
		{
			return field2;
		}

		public List<String> getField3()
		{
			return field3;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(field1, field2, field3, id);
		}

		@Override
		public boolean equals(Object obj)
		{
			if(this == obj) return true;
			if(obj == null) return false;
			if(getClass() != obj.getClass()) return false;
			TestData other = (TestData) obj;
			return Objects.equals(field1, other.field1)
				&& field2 == other.field2
				&& Objects.equals(field3, other.field3)
				&& id == other.id;
		}
	}
}
