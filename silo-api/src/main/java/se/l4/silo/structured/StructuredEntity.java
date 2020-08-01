package se.l4.silo.structured;

import java.util.function.Function;

import se.l4.exobytes.Serializer;
import se.l4.exobytes.Serializers;
import se.l4.exobytes.streaming.StreamingInput;
import se.l4.silo.DeleteResult;
import se.l4.silo.Entity;
import se.l4.silo.FetchResult;
import se.l4.silo.StoreResult;
import se.l4.silo.query.Query;
import se.l4.silo.query.QueryType;

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
	<RT, Q extends Query<?>> Q query(String engine, QueryType<StreamingInput, RT, Q> type);

	/**
	 * Stream everything in this entry.
	 *
	 * @return
	 */
	FetchResult<StreamingInput> stream();

	/**
	 * Get an entity that translates the structured data into objects. This
	 * will call {@link #asObject(Serializer)} with a serializer fetched
	 * from the current {@link Serializers}.
	 *
	 * @param type
	 * @return
	 */
	<T> ObjectEntity<T> asObject(Class<T> type, Function<T, Object> identityMapper);

	/**
	 * Get an entity that translates the structured data into objects.
	 *
	 * @param serializer
	 * @return
	 */
	default <T> ObjectEntity<T> asObject(Serializer<T> serializer, Function<T, Object> identityMapper)
	{
		return new DefaultObjectEntity<>(this, serializer, identityMapper);
	}
}
