package se.l4.silo.engine.builder;

/**
 * Builder to create 
 * @author Andreas Holstenson
 *
 * @param <Parent>
 */
public interface StructuredEntityBuilder<Parent>
	extends HasFieldDefinitions<StructuredEntityBuilder<Parent>>,
		HasQueryEngines<StructuredEntityBuilder<Parent>>
{
	/**
	 * Indicate that we are done building this entity.
	 * 
	 * @return
	 */
	Parent done();
}
