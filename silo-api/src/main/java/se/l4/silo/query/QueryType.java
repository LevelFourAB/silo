package se.l4.silo.query;

import se.l4.commons.serialization.format.StreamingInput;
import se.l4.silo.Entity;
import se.l4.silo.structured.ObjectEntity;
import se.l4.silo.structured.StructuredEntity;

/**
 * Type interface that creates instances of {@link Query}. When an {@link Entity}
 * is queried the query type is responsible for creating a {@link Query} instance
 * that calls a {@link QueryRunner} to execute the query and transform the
 * results.
 * 
 * <p>
 * A {@link QueryType} has three generic attributes, {@code EntityDataType}
 * is the type the entity works with, such as {@link StreamingInput} for
 * {@link StructuredEntity} or any object type for {@link ObjectEntity}.
 * It is assumed that the {@code EntityDataType} can be modified to support
 * composing of queries.
 * 
 * @author Andreas Holstenson
 *
 * @param <ResultType>
 * @param <QueryBuilder>
 */
public interface QueryType<EntityDataType, ResultType, QueryBuilder extends Query<?>>
{
	/**
	 * Create a new query for the given result type.
	 * 
	 * @param type
	 * @param runner
	 * @return
	 */
	QueryBuilder create(QueryRunner<EntityDataType, ResultType> runner);
}
