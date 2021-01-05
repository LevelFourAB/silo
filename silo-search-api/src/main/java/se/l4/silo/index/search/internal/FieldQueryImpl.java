package se.l4.silo.index.search.internal;

import java.util.Objects;

import se.l4.silo.index.Matcher;
import se.l4.silo.index.search.query.FieldQuery;

/**
 * Implementation of {@link FieldQuery}.
 */
public class FieldQueryImpl
	implements FieldQuery
{
	private final String field;
	private final Matcher matcher;
	private final float boost;

	public FieldQueryImpl(
		String field,
		Matcher matcher,
		float boost
	)
	{
		this.field = field;
		this.matcher = matcher;
		this.boost = boost;
	}

	@Override
	public String getField()
	{
		return field;
	}

	@Override
	public Matcher getMatcher()
	{
		return matcher;
	}

	@Override
	public float getBoost()
	{
		return boost;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(boost, field, matcher);
	}

	@Override
	public boolean equals(Object obj)
	{
		if(this == obj) return true;
		if(obj == null) return false;
		if(getClass() != obj.getClass()) return false;
		FieldQueryImpl other = (FieldQueryImpl) obj;
		return Float.floatToIntBits(boost) == Float.floatToIntBits(other.boost)
			&& Objects.equals(field, other.field)
			&& Objects.equals(matcher, other.matcher);
	}

	@Override
	public String toString()
	{
		return "FieldQuery{field=" + field + ", matcher=" + matcher + ", boost=" + boost + "}";
	}
}
