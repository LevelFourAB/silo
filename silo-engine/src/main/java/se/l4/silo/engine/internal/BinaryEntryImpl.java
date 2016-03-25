package se.l4.silo.engine.internal;

import se.l4.commons.io.Bytes;
import se.l4.silo.binary.BinaryEntry;

/**
 * Implementation of {@link BinaryEntry}.
 * 
 * @author Andreas Holstenson
 *
 */
public class BinaryEntryImpl
	implements BinaryEntry
{
	private final Object id;
	private final Object version;
	private final Bytes data;

	public BinaryEntryImpl(Object id, Object version, Bytes data)
	{
		this.id = id;
		this.version = version;
		this.data = data;
	}

	@Override
	public Object getId()
	{
		return id;
	}

	@Override
	public Object getVersion()
	{
		return version;
	}

	@Override
	public Bytes getData()
	{
		return data;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		BinaryEntryImpl other = (BinaryEntryImpl) obj;
		if(id == null)
		{
			if(other.id != null)
				return false;
		}
		else if(!id.equals(other.id))
			return false;
		if(version == null)
		{
			if(other.version != null)
				return false;
		}
		else if(!version.equals(other.version))
			return false;
		return true;
	}

}
