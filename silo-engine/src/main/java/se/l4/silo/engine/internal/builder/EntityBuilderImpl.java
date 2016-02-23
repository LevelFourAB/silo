package se.l4.silo.engine.internal.builder;

import java.util.function.Function;

import se.l4.silo.engine.builder.BinaryBuilder;
import se.l4.silo.engine.builder.EntityBuilder;
import se.l4.silo.engine.builder.StructuredEntityBuilder;
import se.l4.silo.engine.config.EntityConfig;
import se.l4.silo.engine.internal.binary.BinaryBuilderImpl;

/**
 * Implementation of {@link EntityBuilder}.
 * 
 * @author Andreas Holstenson
 *
 * @param <Parent>
 */
public class EntityBuilderImpl<Parent>
	implements EntityBuilder<Parent>
{
	private final Function<EntityConfig, Parent> configReceiver;

	public EntityBuilderImpl(Function<EntityConfig, Parent> configReceiver)
	{
		this.configReceiver = configReceiver;
	}

	@Override
	public BinaryBuilder<Parent> asBinary()
	{
		return new BinaryBuilderImpl<>(configReceiver);
	}

	@Override
	public StructuredEntityBuilder<Parent> asStructured()
	{
		return new StructuredEntityBuilderImpl<>(configReceiver);
	}
}
