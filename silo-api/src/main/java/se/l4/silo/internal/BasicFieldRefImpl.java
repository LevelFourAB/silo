package se.l4.silo.internal;

import java.util.Objects;

import se.l4.silo.index.basic.BasicFieldRef;

/**
 * Implementation of {@link BasicFieldRef}.
 */
public class BasicFieldRefImpl<V>
	implements BasicFieldRef<V>
{
	private final String name;
	private final Class<V> type;

	public BasicFieldRefImpl(String name, Class<V> type)
	{
		this.name = name;
		this.type = type;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, type);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public boolean equals(Object obj)
	{
		if(this == obj) return true;
		if(obj == null) return false;
		if(getClass() != obj.getClass()) return false;
		BasicFieldRefImpl other = (BasicFieldRefImpl) obj;
		return Objects.equals(name, other.name) && Objects.equals(type, other.type);
	}

	@Override
	public String toString()
	{
		return "BasicFieldRefImpl{name=" + name + ", type=" + type + "}";
	}
}
