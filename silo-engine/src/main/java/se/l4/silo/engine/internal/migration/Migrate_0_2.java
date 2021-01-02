package se.l4.silo.engine.internal.migration;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.type.ObjectDataType;

import se.l4.silo.engine.types.DataTypeAdapter;
import se.l4.silo.engine.types.LongFieldType;

/**
 * Class used to migrate a {@link MVStore} from the 0.2 series. Will rename
 * maps used for primary indexes to drop the {@code ::main} part of them.
 */
public class Migrate_0_2
{
	public static void migrate(
		MVStore store
	)
	{
		for(String name : store.getMapNames())
		{
			if(name.startsWith("primary.toExternal."))
			{
				MVMap<?, ?> map = store.openMap(name, new MVMap.Builder<Object, Long>()
					.keyType(new ObjectDataType())
					.valueType(new DataTypeAdapter(LongFieldType.INSTANCE))
				);

				store.renameMap(map, trimStorageName(name));
			}
			else if(name.startsWith("primary.fromExternal."))
			{
				MVMap<?, ?> map = store.openMap(name, new MVMap.Builder<Object, Long>()
					.keyType(new DataTypeAdapter(LongFieldType.INSTANCE))
					.valueType(new ObjectDataType())
				);

				store.renameMap(map, trimStorageName(name));
			}
		}
	}

	private static String trimStorageName(String name)
	{
		int idx = name.lastIndexOf("::");
		return name.substring(0, idx);
	}
}
