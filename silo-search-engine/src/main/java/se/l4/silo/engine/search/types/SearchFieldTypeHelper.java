package se.l4.silo.engine.search.types;

import se.l4.silo.search.SearchIndexException;

public class SearchFieldTypeHelper
{
	private SearchFieldTypeHelper()
	{
	}

	public static Number toNumber(Object o)
	{
		return toNumber(o, "Can not convert to number, got: %s");
	}

	public static Number toNumber(Object o, String errorMessage)
	{
		if(o instanceof Number)
		{
			return (Number) o;
		}

		throw new SearchIndexException(String.format(errorMessage, o));
	}
}
