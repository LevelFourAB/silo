package se.l4.silo.search.query;

import se.l4.silo.search.QueryItem;

public class FieldQuery<R>
	extends AbstractQueryPart<R>
{
	private String field;

	public FieldQuery(String field)
	{
		this.field = field;
	}
	
	public R value(String value)
	{
		receiver.addQuery(new QueryItem("standard", escape(field) + ":\"" + escape(value) + "\""));
		return parent;
	}
	
	/**
	 * Escape a query part.
	 * 
	 * @param in
	 * @return
	 */
	static String escape(String in)
	{
		StringBuilder sb = new StringBuilder(in.length() * 2);
		for(int i=0; i<in.length(); i++)
		{
			char c = in.charAt(i);
			if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '('
				|| c == ')' || c == ':' || c == '^' || c == '[' || c == ']'
				|| c == '\"' || c == '{' || c == '}' || c == '~'
				|| c == '*' || c == '?' || c == '|' || c == '&' || c == '/')
			{
				sb.append('\\');
			}
			sb.append(c);
		}
		return sb.toString();
	}
}
