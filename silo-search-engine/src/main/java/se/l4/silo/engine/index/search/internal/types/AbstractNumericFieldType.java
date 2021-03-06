package se.l4.silo.engine.index.search.internal.types;

import se.l4.silo.engine.index.search.types.NumericFieldType;

/**
 * Abstract implementation of {@link NumericFieldType}.
 */
public abstract class AbstractNumericFieldType<V extends Number>
	implements NumericFieldType<V>
{
	@Override
	public boolean isLocaleSupported()
	{
		return false;
	}

	@Override
	public boolean isDocValuesSupported()
	{
		return true;
	}

	@Override
	public boolean isSortingSupported()
	{
		return true;
	}
}
