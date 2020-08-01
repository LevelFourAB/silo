package se.l4.silo.engine.config;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import se.l4.exobytes.SerializationException;
import se.l4.exobytes.Serializer;
import se.l4.exobytes.SerializerResolver;
import se.l4.exobytes.Serializers;
import se.l4.exobytes.TypeEncounter;
import se.l4.exobytes.Use;
import se.l4.exobytes.streaming.ObjectStreaming;
import se.l4.exobytes.streaming.StreamingInput;
import se.l4.exobytes.streaming.StreamingOutput;
import se.l4.silo.engine.config.ConvertableConfig.ConfigSerializerResolver;

/**
 * Base class for configuration objects that can mutate their type at runtime.
 * This type will store all values when read via serialization so that it
 * can be returned as a more specific type at runtime.
 *
 * @author Andreas Holstenson
 *
 */
@Use(ConfigSerializerResolver.class)
public class ConvertableConfig
{
	private final Map<String, Object> data;
	private final Serializers collection;

	public ConvertableConfig()
	{
		data = null;
		collection = null;
	}

	public ConvertableConfig(Map<String, Object> data, Serializers collection)
	{
		this.data = data;
		this.collection = collection;
	}

	@SuppressWarnings("unchecked")
	public <T> T as(Class<T> type)
	{
		// Void is used when no configuration is required
		if(type == Void.class) return null;

		if(type.isAssignableFrom(getClass()))
		{
			// In case we are already the right type
			return (T) this;
		}

		if(collection == null)
		{
			throw new SerializationException("Can not convert, this instance does not have access to SerializerCollection. Was this instance deserialized?");
		}

		Serializer<T> serializer = collection.get(type);
		try
		{
			return serializer.read(ObjectStreaming.createInput(data));
		}
		catch(IOException e)
		{
			throw new SerializationException("Unable to convert to " + type.getName() + "; " + e.getMessage(), e);
		}
	}

	public static class ConfigSerializerResolver
		implements SerializerResolver<ConvertableConfig>
	{
		@Override
		public Optional<Serializer<ConvertableConfig>> find(TypeEncounter encounter)
		{
			return Optional.of(new ConvertableConfigSerializer(encounter.getCollection()));
		}
	}

	private static class ConvertableConfigSerializer
		implements Serializer<ConvertableConfig>
	{
		private final Serializers collection;

		public ConvertableConfigSerializer(Serializers collection)
		{
			this.collection = collection;
		}

		@Override
		public ConvertableConfig read(StreamingInput in)
			throws IOException
		{
			Map<String, Object> data = (Map) in.readDynamic();
			return new ConvertableConfig(data, collection);
		}

		@Override
		public void write(ConvertableConfig object, StreamingOutput stream)
			throws IOException
		{
		}
	}
}
