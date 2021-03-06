package se.l4.silo.engine.index.search.internal;

import java.io.IOException;
import java.util.function.Function;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;

import se.l4.exobytes.streaming.StreamingInput;
import se.l4.exobytes.streaming.StreamingOutput;
import se.l4.silo.engine.index.search.SearchFieldDefinition;
import se.l4.silo.engine.index.search.facets.FacetCollector;
import se.l4.silo.engine.index.search.types.FieldCreationEncounter;
import se.l4.silo.engine.index.search.types.SearchFieldType;
import se.l4.silo.index.MappableMatcher;
import se.l4.silo.index.Matcher;

/**
 * {@link SearchFieldType} used when mapping a field.
 */
public class MappedSearchFieldType<T, V>
	implements SearchFieldType<V>
{
	protected final SearchFieldType<T> originalType;

	protected final Function<T, V> toV;
	protected final Function<V, T> fromV;

	public MappedSearchFieldType(
		SearchFieldType<T> originalType,
		Function<T, V> toV,
		Function<V, T> fromV
	)
	{
		this.originalType = originalType;
		this.toV = toV;
		this.fromV = fromV;
	}

	@Override
	public boolean isLocaleSupported()
	{
		return originalType.isLocaleSupported();
	}

	@Override
	public boolean isSortingSupported()
	{
		return originalType.isSortingSupported();
	}

	@Override
	public boolean isDocValuesSupported()
	{
		return originalType.isDocValuesSupported();
	}

	@Override
	public void write(V instance, StreamingOutput out)
		throws IOException
	{
		originalType.write(fromV.apply(instance), out);
	}

	@Override
	public V read(StreamingInput in)
		throws IOException
	{
		return toV.apply(originalType.read(in));
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Query createQuery(String field, Matcher<V> matcher)
	{
		Matcher<T> converted;
		if(matcher instanceof MappableMatcher)
		{
			converted = ((MappableMatcher<V>) matcher).map(fromV);
		}
		else
		{
			// Assume that the matcher doesn't depend on a type
			converted = (Matcher) matcher;
		}

		return originalType.createQuery(field, converted);
	}

	@Override
	public void create(FieldCreationEncounter<V> encounter)
	{
		originalType.create(encounter.map(fromV));
	}

	@Override
	public SortField createSortField(String field, boolean ascending)
	{
		return originalType.createSortField(field, ascending);
	}

	public static class Facetable<T, V>
		extends MappedSearchFieldType<T, V>
		implements SearchFieldType.Facetable<V>
	{
		public Facetable(
			SearchFieldType.Facetable<T> originalType,
			Function<T, V> toV,
			Function<V, T> fromV
		)
		{
			super(originalType, toV, fromV);
		}

		@Override
		public FacetCollector<V> createFacetCollector(
			SearchFieldDefinition<?> field
		)
		{
			FacetCollector<T> collector = ((SearchFieldType.Facetable<T>) originalType)
				.createFacetCollector(field);

			return encounter -> collector.collect(encounter.map(toV));
		}
	}
}
