package se.l4.silo.engine;

public interface TransactionValue<V>
{
	/**
	 * Generate this value.
	 *
	 * @param txVersion
	 * @return
	 */
	V generate(long txVersion);

	interface Releasable
	{
		void release();
	}
}
