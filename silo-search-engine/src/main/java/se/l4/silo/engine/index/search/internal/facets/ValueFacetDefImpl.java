package se.l4.silo.engine.index.search.internal.facets;

import java.util.Objects;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;

import se.l4.silo.engine.index.search.SearchFieldDefinition;
import se.l4.silo.engine.index.search.facets.FacetCollector;
import se.l4.silo.engine.index.search.facets.ValueFacetDef;
import se.l4.silo.engine.index.search.types.SearchFieldType;
import se.l4.silo.index.search.SearchIndexException;
import se.l4.silo.index.search.facets.ValueFacetQuery;

public class ValueFacetDefImpl<T, V>
	implements ValueFacetDef<T, V>
{
	private final String id;
	private final SearchFieldDefinition<T> field;

	public ValueFacetDefImpl(
		String id,
		SearchFieldDefinition<T> field
	)
	{
		this.id = id;
		this.field = field;
	}

	@Override
	public String getId()
	{
		return id;
	}

	@Override
	public ListIterable<? extends SearchFieldDefinition<T>> getFields()
	{
		return Lists.immutable.of(field);
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public FacetCollector<V> createCollector(ValueFacetQuery<V> query)
	{
		return ((SearchFieldType.Facetable) field.getType()).createFacetCollector(field);
	}

	public static <T> Builder<T, ?> create(Class<T> type, String name)
	{
		return new BuilderImpl<>(name, null);
	}

	public static class BuilderImpl<T, V>
		implements Builder<T, V>
	{
		private final String id;
		private final SearchFieldDefinition<T> field;

		public BuilderImpl(
			String id,
			SearchFieldDefinition<T> field
		)
		{
			this.id = id;
			this.field = field;
		}

		@Override
		public <NV> Builder<T, NV> withField(SearchFieldDefinition<T> field)
		{
			Objects.requireNonNull(field);

			if(! field.getType().isDocValuesSupported()
				|| ! (field.getType() instanceof SearchFieldType.Facetable))
			{
				throw new SearchIndexException("Field does not support facets");
			}

			return new BuilderImpl<>(
				id,
				field
			);
		}

		@Override
		public ValueFacetDef<T, V> build()
		{
			Objects.requireNonNull(field, "field must be specified");

			return new ValueFacetDefImpl<>(id, field);
		}
	}
}
