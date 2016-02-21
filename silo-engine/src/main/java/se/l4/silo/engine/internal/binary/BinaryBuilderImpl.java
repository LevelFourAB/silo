package se.l4.silo.engine.internal.binary;

import java.util.function.Function;

import se.l4.silo.engine.builder.BinaryBuilder;
import se.l4.silo.engine.config.EntityConfig;

/**
 * Implementation of {@link BinaryBuilder}.
 * 
 * @author Andreas Holstenson
 *
 * @param <Parent>
 */
public class BinaryBuilderImpl<Parent>
	implements BinaryBuilder<Parent>
{
	private Function<EntityConfig, Parent> configReceiver;
	private EntityConfig config;

	public BinaryBuilderImpl(Function<EntityConfig, Parent> configReceiver)
	{
		this.configReceiver = configReceiver;
		
		config = new EntityConfig("silo:binary");
	}

	@Override
	public Parent done()
	{
		return configReceiver.apply(config);
	}
}
