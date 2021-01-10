package se.l4.silo.engine;

import java.time.Duration;

import se.l4.silo.Silo;

/**
 * Maintenance for a {@link Silo} instance.
 */
public interface Maintenance
{
	/**
	 * Compact the storage.
	 *
	 * @param maxTime
	 *   the maximum time to spend compacting
	 */
	void compact(Duration maxTime);

	/**
	 * Create a snapshot of this instance.
	 *
	 * @return
	 */
	Snapshot createSnapshot();
}
