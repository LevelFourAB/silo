package se.l4.silo.engine.internal;

import se.l4.silo.engine.LocalSilo;

public abstract class BasicTest
	extends SiloTest
{
	protected abstract LocalSilo.Builder setup(LocalSilo.Builder builder);

	public LocalSilo instance()
	{
		return instance(this::setup);
	}
}
