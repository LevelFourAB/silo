package se.l4.silo.engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import se.l4.exobytes.Serializer;
import se.l4.exobytes.Serializers;
import se.l4.silo.engine.internal.SerializingCodec;

/**
 * Codec used to map between object and binary format.
 */
public interface ObjectCodec<T>
{
	/**
	 * Encode the given instance.
	 *
	 * @param instance
	 * @param out
	 * @throws IOException
	 */
	void encode(T instance, OutputStream out)
		throws IOException;

	/**
	 * Decode the given stream of bytes.
	 *
	 * @param in
	 * @return
	 * @throws IOException
	 */
	T decode(InputStream in)
		throws IOException;

	/**
	 * Get a codec using the given {@link Serializer}.
	 *
	 * @param <T>
	 * @param serializer
	 * @return
	 */
	public static <T> ObjectCodec<T> serialized(Serializer<T> serializer)
	{
		return new SerializingCodec<>(serializer);
	}

	/**
	 * Get a codec using the given {@link Serializers} and type name.
	 *
	 * @param <T>
	 * @param serializer
	 * @return
	 */
	public static <T> ObjectCodec<T> serialized(Serializers serializers, Class<T> type)
	{
		return serialized(serializers.get(type));
	}
}
