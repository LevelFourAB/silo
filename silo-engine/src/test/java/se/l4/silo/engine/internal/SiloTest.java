package se.l4.silo.engine.internal;

import java.nio.file.Path;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;

import se.l4.exobytes.Serializers;
import se.l4.silo.Silo;
import se.l4.silo.engine.LocalSilo;

/**
 * Base class for tests that need access to a {@link Silo} instance.
 */
public abstract class SiloTest
{
	protected static Serializers serializers = Serializers.create().build();

	@TempDir
	protected Path tmp;

	private LocalSilo silo;

	@AfterEach
	public void after()
		throws Exception
	{
		if(silo != null)
		{
			silo.close();
		}
	}

	protected LocalSilo instance(Function<LocalSilo.Builder, LocalSilo.Builder> creator)
	{
		if(silo != null)
		{
			return silo;
		}

		silo = creator.apply(LocalSilo.open(tmp))
			.start()
			.block();

		return silo;
	}
}
