package se.l4.silo.engine.index.search.types;

/**
 * Abstract base for field types for numeric values.
 */
public abstract class NumericFieldType<T extends Number>
	implements SearchFieldType<T>
{
	public NumericFieldType()
	{
	}

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
