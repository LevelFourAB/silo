package se.l4.silo;

/**
 * Type interface that creates instances of {@link Query}.
 * 
 * @author Andreas Holstenson
 *
 * @param <ResultType>
 * @param <R>
 */
public interface QueryType<ResultType, R extends Query<?>>
{
	/**
	 * Create a new query for the given result type.
	 * 
	 * @param type
	 * @param runner
	 * @return
	 */
	R create(Class<ResultType> type, QueryRunner<ResultType> runner);
}
