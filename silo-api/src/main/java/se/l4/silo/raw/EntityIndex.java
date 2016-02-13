package se.l4.silo.raw;

import java.util.List;

import se.l4.silo.binary.BinaryEntity;
import se.l4.silo.binary.BinaryEntry;

/**
 * Index information for a {@link BinaryEntity}.
 * 
 * @author Andreas Holstenson
 */
public interface EntityIndex
{
	/**
	 * Get the name of the index.
	 * 
	 * @return
	 */
	String getName();
	
	/**
	 * Get the names of all fields that are available in this index.
	 * 
	 * @return
	 */
	List<String> getFields();
	
	/**
	 * Get fields that should can be sorted upon.
	 * 
	 * @return
	 */
	List<String> getSortFields();
	
	/**
	 * Perform a query on this index.
	 * 
	 * @param arguments
	 */
	EntityIndexQuery<BinaryEntry> query();
}
