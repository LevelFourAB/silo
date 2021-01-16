package se.l4.silo.engine.types;

import java.io.IOException;

import org.eclipse.collections.api.map.primitive.IntObjectMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;

import se.l4.silo.engine.io.ExtendedDataInput;
import se.l4.silo.engine.io.ExtendedDataOutput;

public class VersionedType<T>
	implements FieldType<T>
{
	private final IntObjectMap<FieldType<T>> versions;
	private int latest;
	private FieldType<T> latestType;

	private VersionedType(IntObjectMap<FieldType<T>> versions)
	{
		this.versions = versions;
		int max = (int) versions.keysView().reduce((curr, v) -> v > curr ? v : curr);

		this.latest = max;
		this.latestType = versions.get(latest);
	}

	public static <T> FieldType<T> singleVersion(FieldType<T> other)
	{
		IntObjectHashMap<FieldType<T>> versions = new IntObjectHashMap<>();
		versions.put(1, other);
		return new VersionedType<>(versions);
	}

	@Override
	public int compare(T o1, T o2)
	{
		return latestType.compare(o1, o2);
	}

	@Override
	public int estimateMemory(T instance)
	{
		return latestType.estimateMemory(instance);
	}

	@Override
	public T convert(Object in)
	{
		return latestType.convert(in);
	}

	@Override
	public void write(T instance, ExtendedDataOutput out) throws IOException
	{
		out.writeVInt(latest);
		latestType.write(instance, out);
	}

	@Override
	public T read(ExtendedDataInput in) throws IOException
	{
		int version = in.readVInt();
		FieldType<T> ft = versions.get(version);
		if(ft == null)
		{
			throw new IOException("Unsupported version, no type registered for " + version);
		}
		return ft.read(in);
	}

	@Override
	public T nextDown(T in)
	{
		throw new UnsupportedOperationException("versioned types can not be compared");
	}

	@Override
	public T nextUp(T in)
	{
		throw new UnsupportedOperationException("versioned types can not be compared");
	}
}
