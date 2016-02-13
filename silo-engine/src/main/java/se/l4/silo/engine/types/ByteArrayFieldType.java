package se.l4.silo.engine.types;

import java.io.IOException;

import se.l4.aurochs.core.io.ExtendedDataInput;
import se.l4.aurochs.core.io.ExtendedDataOutput;
import se.l4.aurochs.serialization.Serializer;

public class ByteArrayFieldType
	implements FieldType<byte[]>
{
	public static final FieldType<byte[]> INSTANCE = new ByteArrayFieldType();

	@Override
	public String uniqueId()
	{
		return "byte-array";
	}

	@Override
	public int compare(byte[] o1, byte[] o2)
	{
		return 0;
	}

	@Override
	public int estimateMemory(byte[] instance)
	{
		return 64 + instance.length * 4;
	}

	@Override
	public byte[] convert(Object in)
	{
		return (byte[]) in;
	}

	@Override
	public Serializer<byte[]> getSerializer()
	{
		return null;
	}

	@Override
	public void write(byte[] instance, ExtendedDataOutput out)
		throws IOException
	{
		out.writeVInt(instance.length);
		out.write(instance);
	}

	@Override
	public byte[] read(ExtendedDataInput in)
		throws IOException
	{
		int length = in.readVInt();
		byte[] result = new byte[length];
		in.readFully(result);
		return result;
	}

}
