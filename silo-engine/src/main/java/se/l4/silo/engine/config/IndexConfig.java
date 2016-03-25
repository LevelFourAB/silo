package se.l4.silo.engine.config;

import se.l4.commons.serialization.Expose;
import se.l4.commons.serialization.ReflectionSerializer;
import se.l4.commons.serialization.Use;
import se.l4.silo.engine.IndexQueryEngineFactory;

/**
 * Configuration for {@link IndexQueryEngineFactory}.
 * 
 * @author Andreas Holstenson
 *
 */
@Use(ReflectionSerializer.class)
public class IndexConfig
	extends QueryEngineConfig
{
	@Expose
	private String[] fields;
	@Expose
	private String[] sortFields;
	
	public IndexConfig()
	{
	}
	
	public IndexConfig(String[] fields, String[] sortFields)
	{
		super("silo:index");
		
		this.fields = fields;
		this.sortFields = sortFields;
	}
	
	/**
	 * Get the fields of the index.
	 * 
	 * @return
	 */
	public String[] getFields()
	{
		return fields;
	}
	
	/**
	 * Get the fields that can be sorted on.
	 * 
	 * @return
	 */
	public String[] getSortFields()
	{
		return sortFields;
	}
}
