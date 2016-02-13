package se.l4.silo.engine;

import se.l4.aurochs.core.io.Bytes;
import se.l4.aurochs.serialization.format.StreamingInput;

public interface DataEncounter
{
	/**
	 * Get the binary data of this encounter.
	 * 
	 * @return
	 */
	Bytes getData();
	
	/**
	 * Get as structured data.
	 * 
	 * @return
	 */
	StreamingInput asStructured();
}
