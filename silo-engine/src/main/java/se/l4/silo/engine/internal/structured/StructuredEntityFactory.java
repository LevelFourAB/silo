package se.l4.silo.engine.internal.structured;

import se.l4.silo.engine.EntityCreationEncounter;
import se.l4.silo.engine.EntityTypeFactory;
import se.l4.silo.engine.Storage;
import se.l4.silo.engine.config.StructuredEntityConfig;
import se.l4.silo.structured.StructuredEntity;

/**
 * Factory for creating instances of {@link StructuredEntity}.
 * 
 * @author Andreas Holstenson
 *
 */
public class StructuredEntityFactory
	implements EntityTypeFactory<StructuredEntity, StructuredEntityConfig>
{

	@Override
	public String getId()
	{
		return "silo:structured";
	}

	@Override
	public Class<StructuredEntityConfig> getConfigType()
	{
		return StructuredEntityConfig.class;
	}
	
	@Override
	public StructuredEntity create(EntityCreationEncounter<StructuredEntityConfig> encounter)
	{
		Storage entity = encounter.createMainEntity()
			.withQueryEngines(encounter.getConfig())
			.build();
		return new StructuredEntityImpl(encounter.getEntityName(), entity);
	}
	
}
