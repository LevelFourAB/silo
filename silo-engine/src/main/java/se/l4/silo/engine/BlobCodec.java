package se.l4.silo.engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import se.l4.silo.Blob;
import se.l4.ylem.io.Bytes;

/**
 * {@link EntityCodec} that encodes to and from {@link Blob}. Used when an
 * entity is intended to store binary data that can be lazy-loaded.
 */
public class BlobCodec<ID>
	implements EntityCodec<Blob<ID>>
{
	@Override
	public Blob<ID> decode(InputStream in)
		throws IOException
	{
		Bytes bytes = Bytes.capture(in);
		return Blob.create(null, bytes);
	}

	@Override
	public void encode(Blob<ID> instance, OutputStream out)
		throws IOException
	{
		try(InputStream in = instance.openStream())
		{
			in.transferTo(out);
		}
	}
}
