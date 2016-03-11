package se.l4.silo.structured;

import se.l4.aurochs.serialization.Serializer;
import se.l4.aurochs.serialization.format.StreamingInput;
import se.l4.silo.DeleteResult;
import se.l4.silo.Entity;
import se.l4.silo.FetchResult;
import se.l4.silo.Query;
import se.l4.silo.QueryType;
import se.l4.silo.StoreResult;

/**
 * Entity for storing structured data. Anything that can be represented as
 * a {@link StreamingInput} is considered structured data.
 * 
 * @author Andreas Holstenson
 *
 */
public interface StructuredEntity
	extends Entity
{
	/**
	 * Get data stored for the given identifier.
	 * 
	 * @param id
	 * @return
	 */
	FetchResult<StreamingInput> get(Object id);
	
	/**
	 * Store new data and associate it with the given identifier.
	 * 
	 * @param id
	 * @param out
	 * @return
	 */
	StoreResult store(Object id, StreamingInput out);
	
	/**
	 * Delete data associated with the given identifier.
	 * 
	 * @param id
	 * @return
	 */
	DeleteResult delete(Object id);
	
	/**
	 * Query the the query engine.
	 * 
	 * @param index
	 * @return
	 */
	<RT, Q extends Query<RT>> Q query(String engine, QueryType<StreamingInput, RT, Q> type);

	/**
	 * Get an entity that translates the structured data into objects.
	 * 
	 * @param serializer
	 * @return
	 */
	default <T> ObjectEntity<T> asObject(Serializer<T> serializer)
	{
		return new DefaultObjectEntity<>(this, serializer);
	}
}
