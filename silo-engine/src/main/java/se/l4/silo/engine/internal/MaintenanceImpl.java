package se.l4.silo.engine.internal;

import java.time.Duration;

import se.l4.silo.engine.Maintenance;
import se.l4.silo.engine.Snapshot;

public class MaintenanceImpl
	implements Maintenance
{
	private final StorageEngine storageEngine;

	public MaintenanceImpl(
		StorageEngine storageEngine
	)
	{
		this.storageEngine = storageEngine;
	}

	@Override
	public Snapshot createSnapshot()
	{
		return storageEngine.createSnapshot();
	}

	@Override
	public void compact(Duration maxTime)
	{
		storageEngine.compact(maxTime.toMillis());
	}
}
