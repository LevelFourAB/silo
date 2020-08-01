package se.l4.silo.engine.io;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

import org.checkerframework.checker.nullness.qual.NonNull;

import se.l4.ylem.io.Bytes;

public class ExtendedDataOutputStream
	extends DataOutputStream
	implements ExtendedDataOutput
{
	public ExtendedDataOutputStream(OutputStream out)
	{
		super(out);
	}

	@Override
	public void writeVInt(int value)
		throws IOException
	{
		while(true)
		{
			if((value & ~0x7F) == 0)
			{
				write(value);
				break;
			}
			else
			{
				write((value & 0x7f) | 0x80);
				value >>>= 7;
			}
		}
	}

	@Override
	public void writeVLong(long value)
		throws IOException
	{
		while(true)
		{
			if((value & ~0x7FL) == 0)
			{
				write((int) value);
				break;
			}
			else
			{
				write(((int) value & 0x7f) | 0x80);
				value >>>= 7;
			}
		}
	}

	@Override
	public void writeString(@NonNull String string)
		throws IOException
	{
		Objects.requireNonNull(string);

		writeVInt(string.length());
		for(int i=0, n=string.length(); i<n; i++)
		{
			char c = string.charAt(i);
			if(c <= 0x007f)
			{
				out.write((byte) c);
			}
			else if(c > 0x07ff)
			{
				out.write((byte) (0xe0 | c >> 12 & 0x0f));
				out.write((byte) (0x80 | c >> 6 & 0x3f));
				out.write((byte) (0x80 | c >> 0 & 0x3f));
			}
			else
			{
				out.write((byte) (0xc0 | c >> 6 & 0x1f));
				out.write((byte) (0x80 | c >> 0 & 0x3f));
			}
		}
	}

	@Override
	public void writeBytes(Bytes bytes)
		throws IOException
	{
		Objects.requireNonNull(bytes);

		bytes.asChunks(8192, (data, offset, len) -> {
			writeVInt(len);
			write(data, offset, len);
		});
		writeVInt(0);
	}
}
