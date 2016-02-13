package se.l4.silo;

import java.io.IOException;

import se.l4.aurochs.serialization.format.StreamingOutput;

public interface Metadata
{
	/**
	 * Write this metadata to the given output.
	 * 
	 * @param out
	 * @throws IOException
	 */
	void write(StreamingOutput out)
		throws IOException;
}
