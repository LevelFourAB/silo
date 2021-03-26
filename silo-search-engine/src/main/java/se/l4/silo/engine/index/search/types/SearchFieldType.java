package se.l4.silo.engine.index.search.types;

import java.io.IOException;
import java.util.function.Function;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;

import se.l4.exobytes.streaming.StreamingInput;
import se.l4.exobytes.streaming.StreamingOutput;
import se.l4.silo.engine.Buildable;
import se.l4.silo.engine.index.search.SearchFieldDef;
import se.l4.silo.engine.index.search.facets.FacetCollector;
import se.l4.silo.engine.index.search.facets.ValueFacetDef;
import se.l4.silo.engine.index.search.internal.MappedSearchFieldType;
import se.l4.silo.engine.index.search.internal.types.BinaryFieldType;
import se.l4.silo.engine.index.search.internal.types.BooleanFieldType;
import se.l4.silo.engine.index.search.internal.types.DoubleFieldType;
import se.l4.silo.engine.index.search.internal.types.FieldTypeInstanceBuilder;
import se.l4.silo.engine.index.search.internal.types.FloatFieldType;
import se.l4.silo.engine.index.search.internal.types.IntFieldType;
import se.l4.silo.engine.index.search.internal.types.LongFieldType;
import se.l4.silo.index.Matcher;

/**
 * Type of data that can be used for {@link SearchFieldDef fields}.
 */
public interface SearchFieldType<V>
{
	/**
	 * Write the given instance to the output.
	 *
	 * @param instance
	 * @param out
	 * @throws IOException
	 */
	void write(V instance, StreamingOutput out)
		throws IOException;

	/**
	 * Read an instance from the given input.
	 *
	 * @param instance
	 * @param in
	 * @return
	 * @throws IOException
	 */
	V read(StreamingInput in)
		throws IOException;

	/**
	 * Get if this type supports different variants based on locales.
	 *
	 * @return
	 */
	boolean isLocaleSupported();

	/**
	 * Get if this type supports sorting.
	 *
	 * @return
	 */
	boolean isSortingSupported();

	/**
	 * Get if this type supports doc values.
	 *
	 * @return
	 */
	boolean isDocValuesSupported();

	/**
	 * Create a query for the given instance of {@link Matcher}.
	 *
	 * @param matcher
	 * @return
	 */
	Query createQuery(String field, Matcher<V> matcher);

	/**
	 * Create the field from the given object.
	 *
	 * @param object
	 * @return
	 */
	void create(
		FieldCreationEncounter<V> encounter
	);

	/**
	 * Create a {@link SortField} for this type.
	 *
	 * @param field
	 * @param ascending
	 * @param params
	 * @return
	 */
	default SortField createSortField(String field, boolean ascending)
	{
		throw new UnsupportedOperationException("The field type " + getClass().getSimpleName() + " does not support sorting");
	}

	/**
	 * Map this type using the given functions. This can be used to create
	 * field types that use high level objects (such as {@link java.time.ZonedDateTime})
	 * but stores them using a low-level field type (such as `long`).
	 *
	 * @param <NV>
	 * @param toN
	 * @param fromN
	 * @return
	 */
	default <NV> SearchFieldType<NV> map(
		Function<V, NV> toN,
		Function<NV, V> fromN
	)
	{
		return new MappedSearchFieldType<>(this, toN, fromN);
	}

	/**
	 * Extension to {@link SearchFieldType} for those types that can provide
	 * distinct values for use with a {@link ValueFacetDef}.
	 */
	interface Facetable<V>
		extends SearchFieldType<V>
	{
		/**
		 * Create a collector for facet values.
		 *
		 * @param fieldName
		 *   the field where DocValues is stored
		 * @param encounter
		 *   the encounter to use
		 * @return
		 */
		FacetCollector<V> createFacetCollector(
			SearchFieldDef<?> field
		);

		@Override
		default <NV> SearchFieldType.Facetable<NV> map(
			Function<V, NV> toN,
			Function<NV, V> fromN
		)
		{
			return new MappedSearchFieldType.Facetable<>(this, toN, fromN);
		}
	}

	/**
	 * Builder for a {@link SearchFieldType} instance.
	 */
	interface Builder<V, T extends SearchFieldType<V>>
		extends Buildable<T>
	{
		/**
		 * Map this field type.
		 *
		 * @param <NV>
		 * @param toN
		 * @param fromN
		 * @return
		 */
		default <NV> Builder<NV, SearchFieldType<NV>> map(
			Function<V, NV> toN,
			Function<NV, V> fromN
		)
		{
			return new FieldTypeInstanceBuilder<>(build().map(toN, fromN));
		}
	}

	/**
	 * Start building a type for binary values.
	 *
	 * @return
	 */
	static Builder<byte[], SearchFieldType<byte[]>> forBinary()
	{
		return new FieldTypeInstanceBuilder<>(BinaryFieldType.INSTANCE);
	}

	/**
	 * Start building a type for string values.
	 *
	 * @return
	 */
	static StringFieldType.Builder forString()
	{
		return StringFieldType.create();
	}

	/**
	 * Start building a type for float values.
	 *
	 * @return
	 */
	static Builder<Float, NumericFieldType<Float>> forFloat()
	{
		return new FieldTypeInstanceBuilder<>(FloatFieldType.INSTANCE);
	}

	/**
	 * Start building a type for double values.
	 *
	 * @return
	 */
	static Builder<Double, NumericFieldType<Double>> forDouble()
	{
		return new FieldTypeInstanceBuilder<>(DoubleFieldType.INSTANCE);
	}

	/**
	 * Start building a type for integer values.
	 *
	 * @return
	 */
	static Builder<Integer, NumericFieldType<Integer>> forInteger()
	{
		return new FieldTypeInstanceBuilder<>(IntFieldType.INSTANCE);
	}

	/**
	 * Start building a type for long values.
	 *
	 * @return
	 */
	static Builder<Long, NumericFieldType<Long>> forLong()
	{
		return new FieldTypeInstanceBuilder<>(LongFieldType.INSTANCE);
	}

	/**
	 * Start building a type for boolean values.
	 *
	 * @return
	 */
	static Builder<Boolean, SearchFieldType<Boolean>> forBoolean()
	{
		return new FieldTypeInstanceBuilder<>(BooleanFieldType.INSTANCE);
	}
}
