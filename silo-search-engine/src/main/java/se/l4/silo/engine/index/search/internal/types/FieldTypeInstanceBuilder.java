package se.l4.silo.engine.index.search.internal.types;

import se.l4.silo.engine.index.search.types.SearchFieldType;

/**
 * Implementation of {@link SearchFieldType.Builder} that resolves to a
 * specific instance.
 */
public class FieldTypeInstanceBuilder<V, T extends SearchFieldType<V>>
	implements SearchFieldType.Builder<V, T>
{
	private final T instance;

	public FieldTypeInstanceBuilder(T instance)
	{
		this.instance = instance;
	}

	@Override
	public T build()
	{
		return instance;
	}
}
