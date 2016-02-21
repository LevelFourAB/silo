package se.l4.silo.engine;

import se.l4.aurochs.core.io.Bytes;
import se.l4.silo.DeleteResult;
import se.l4.silo.StoreResult;

public interface Storage
{
	StoreResult store(Object id, Bytes bytes);
	
	Bytes get(Object id);

	DeleteResult delete(Object id);
}
