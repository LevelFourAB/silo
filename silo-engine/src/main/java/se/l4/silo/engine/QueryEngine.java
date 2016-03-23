package se.l4.silo.engine;

import java.io.Closeable;

/**
 * Engine that provides query abilities for stored data.
 * 
 * @author Andreas Holstenson
 *
 */
public interface QueryEngine<T>
	extends Closeable
{
	/**
	 * Query this query engine.
	 * 
	 * @param encounter
	 */
	void query(QueryEncounter<T> encounter);
	
	/**
	 * Update this query engine with new data.
	 * 
	 * @param id
	 * @param encounter
	 */
	void update(long id, DataEncounter encounter);
	
	/**
	 * Delete something from this query engine.
	 * 
	 * @param id
	 */
	void delete(long id);
}
