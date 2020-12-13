package se.l4.silo.engine.internal.field;

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
import se.l4.silo.engine.index.FieldDefinition;
import se.l4.silo.engine.index.FieldIndexDefinition;
import se.l4.silo.engine.internal.BasicTest;
import se.l4.silo.index.FieldIndexQuery;
import se.l4.silo.index.FieldIndexResult;

public class FieldIndexQueryEngineTest
	extends BasicTest
{
	@Override
	protected LocalSilo.Builder setup(LocalSilo.Builder builder)
	{
		FieldDefinition<TestData> field1 = FieldDefinition.create("field1", TestData.class)
			.withType(String.class)
			.withSupplier(TestData::getField1)
			.build();

		FieldDefinition<TestData> field2 = FieldDefinition.create("field2", TestData.class)
			.withType(boolean.class)
			.withSupplier(TestData::isField2)
			.build();

		FieldDefinition<TestData> field3 = FieldDefinition.create("field3", TestData.class)
			.withType(String.class)
			.collection()
			.withSupplier(TestData::getField3)
			.build();

		EntityDefinition<Long, TestData> test = EntityDefinition.create("test", TestData.class)
			.withCodec(EntityCodec.serialized(serializers, TestData.class))
			.withId(Long.class, TestData::getId)
			.addIndex(FieldIndexDefinition.create("byField1", TestData.class)
				.addField(field1)
				.build()
			)
			.addIndex(FieldIndexDefinition.create("byField2", TestData.class)
				.addField(field2)
				.build()
			)
			.addIndex(FieldIndexDefinition.create("byField3", TestData.class)
				.addField(field3)
				.build()
			)
			.addIndex(FieldIndexDefinition.create("multiple", TestData.class)
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

		FieldIndexResult<TestData> fr = entity.fetch(FieldIndexQuery.create("byField1", TestData.class)
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

		FieldIndexResult<TestData> fr = entity.fetch(FieldIndexQuery.create("byField1", TestData.class)
			.field("field1").isEqualTo("value1")
			.build()
		).block();

		assertThat(fr.getSize(), is(1l));

		entity.delete(1l).block();

		fr = entity.fetch(FieldIndexQuery.create("byField1", TestData.class)
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

		FieldIndexResult<TestData> fr = entity.fetch(FieldIndexQuery.create("byField1", TestData.class)
			.field("field1").isEqualTo("value1")
			.build()
		).block();

		assertThat(fr.getSize(), is(1l));

		// Replace with new data
		obj = new TestData(1, "value2", false, Collections.emptyList());
		entity.store(obj).block();

		// value1 should no longer match
		fr = entity.fetch(FieldIndexQuery.create("byField1", TestData.class)
			.field("field1").isEqualTo("value1")
			.build()
		).block();

		assertThat(fr.getSize(), is(0l));

		// value2 should now match
		fr = entity.fetch(FieldIndexQuery.create("byField1", TestData.class)
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

		FieldIndexResult<TestData> fr = entity.fetch(FieldIndexQuery.create("byField1", TestData.class)
			.field("field1").isEqualTo("value1")
			.build()
		).block();

		assertThat(fr.getSize(), is(2l));
	}

	@Test
	public void testStoreMultiple2()
	{
		Entity<Long, TestData> entity = entity();

		instance().inTransaction(() -> {
			TestData obj1 = new TestData(1, "value2", false, Collections.emptyList());
			entity.store(obj1).block();
			TestData obj2 = new TestData(2, "value2", true, Collections.emptyList());
			entity.store(obj2).block();
		});

		FieldIndexResult<TestData> fr = entity.fetch(FieldIndexQuery.create("byField1", TestData.class)
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

		FieldIndexResult<TestData> fr = entity.fetch(FieldIndexQuery.create("byField1", TestData.class)
			.field("field1").isEqualTo("value1")
			.build()
		).block();

		assertThat(fr.getSize(), is(1l));

		TestData obj2 = new TestData(1, "value2", false, Collections.emptyList());
		entity.store(obj2).block();

		fr = entity.fetch(FieldIndexQuery.create("byField1", TestData.class)
			.field("field1").isEqualTo("value1")
			.build()
		).block();

		assertThat(fr.getSize(), is(0l));

		TestData obj3 = new TestData(1, "value1", false, Collections.emptyList());
		entity.store(obj3).block();

		fr = entity.fetch(FieldIndexQuery.create("byField1", TestData.class)
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

		FieldIndexResult<TestData> fr = entity.fetch(FieldIndexQuery.create("byField3", TestData.class)
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
