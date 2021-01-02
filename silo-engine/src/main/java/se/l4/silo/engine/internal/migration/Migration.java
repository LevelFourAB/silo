package se.l4.silo.engine.internal.migration;

import org.h2.mvstore.MVStore;

/**
 * Migration support for stores. Used to provide backward compatibility with
 * old data files.
 */
public class Migration
{
	private static final int CURRENT = 1;

	private Migration()
	{
	}

	public static void migrate(MVStore store)
	{
		if(store.getCurrentVersion() == 0)
		{
			// If there's no data written set latest version and return
			store.setStoreVersion(CURRENT);
			return;
		}

		int version = store.getStoreVersion();
		switch(version)
		{
			case 0:
				// This store is from the 0.2 series
				Migrate_0_2.migrate(store);
				store.setStoreVersion(CURRENT);
				break;
			case 1:
				// Current version - do nothing
				break;
		}
	}
}
