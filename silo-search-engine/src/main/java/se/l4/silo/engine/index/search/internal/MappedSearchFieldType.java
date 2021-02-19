package se.l4.silo.engine.index.search.internal;

import java.io.IOException;
import java.util.function.Function;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;

import se.l4.exobytes.streaming.StreamingInput;
import se.l4.exobytes.streaming.StreamingOutput;
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
	private final SearchFieldType<T> originalType;

	private final Function<T, V> toV;
	private final Function<V, T> fromV;

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
}
