package se.l4.silo.engine.internal.tx.operations;

/**
 * Operation within a transaction.
 */
public interface TransactionOperation
{
	/**
	 * Estimate the amount of memory used.
	 *
	 * @return
	 */
	int estimateMemory();

	/**
	 * Estimate the memory used for an identifier.
	 *
	 * @param id
	 * @return
	 */
	static int estimateIdMemory(Object id)
	{
		if(id instanceof Long)
		{
			return 30;
		}
		else if(id instanceof Integer)
		{
			return 24;
		}
		else if(id instanceof String)
		{
			return 24 + 2 * ((String) id).length();
		}
		else
		{
			return 8;
		}
	}
}
