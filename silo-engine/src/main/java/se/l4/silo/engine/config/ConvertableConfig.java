package se.l4.silo.engine.config;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import se.l4.commons.config.ConfigException;
import se.l4.commons.config.internal.RawFormatReader;
import se.l4.commons.config.internal.streaming.MapInput;
import se.l4.commons.serialization.Serializer;
import se.l4.commons.serialization.SerializerCollection;
import se.l4.commons.serialization.Use;
import se.l4.commons.serialization.format.StreamingInput;
import se.l4.commons.serialization.format.StreamingOutput;
import se.l4.commons.serialization.spi.SerializerResolver;
import se.l4.commons.serialization.spi.TypeEncounter;
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
	private final SerializerCollection collection;

	public ConvertableConfig()
	{
		data = null;
		collection = null;
	}

	public ConvertableConfig(Map<String, Object> data, SerializerCollection collection)
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
			throw new ConfigException("Can not convert, this instance does not have access to SerializerCollection. Was this instance deserialized?");
		}

		Serializer<T> serializer = collection.find(type);
		MapInput in = new MapInput("", data);
		try
		{
			return serializer.read(in);
		}
		catch(IOException e)
		{
			throw new ConfigException("Unable to convert to " + type.getName() + "; " + e.getMessage(), e);
		}
	}

	public static class ConfigSerializerResolver
		implements SerializerResolver<ConvertableConfig>
	{

		@Override
		public Serializer<ConvertableConfig> find(TypeEncounter encounter)
		{
			return new ConvertableConfigSerializer(encounter.getCollection());
		}

		@Override
		public Set<Class<? extends Annotation>> getHints()
		{
			return Collections.emptySet();
		}

	}

	private static class ConvertableConfigSerializer
		implements Serializer<ConvertableConfig>
	{
		private final SerializerCollection collection;

		public ConvertableConfigSerializer(SerializerCollection collection)
		{
			this.collection = collection;
		}

		@Override
		public ConvertableConfig read(StreamingInput in)
			throws IOException
		{
			Map<String, Object> data = RawFormatReader.read(in);
			return new ConvertableConfig(data, collection);
		}

		@Override
		public void write(ConvertableConfig object, String name, StreamingOutput stream)
			throws IOException
		{
		}

	}
}
