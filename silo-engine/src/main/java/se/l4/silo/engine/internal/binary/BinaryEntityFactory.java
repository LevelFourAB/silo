package se.l4.silo.engine.internal.binary;

import se.l4.silo.binary.BinaryEntity;
import se.l4.silo.engine.EntityCreationEncounter;
import se.l4.silo.engine.EntityTypeFactory;
import se.l4.silo.engine.Storage;

/**
 * Factory for creating instances of {@link BinaryEntity}.
 * 
 * @author Andreas Holstenson
 *
 */
public class BinaryEntityFactory
	implements EntityTypeFactory<BinaryEntity, Void>
{
	@Override
	public String getId()
	{
		return "silo:binary";
	}
	
	@Override
	public Class<Void> getConfigType()
	{
		return Void.class;
	}

	@Override
	public BinaryEntity create(EntityCreationEncounter<Void> encounter)
	{
		Storage storage = encounter.createMainEntity().build();
		return new BinaryEntityImpl(encounter.getEntityName(), storage);
	}
}
