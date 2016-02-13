package se.l4.silo.engine;

import se.l4.aurochs.core.io.Bytes;

public interface DataEncounter
{
	/**
	 * Get the binary data of this encounter.
	 * 
	 * @return
	 */
	Bytes getData();
}
