package se.l4.silo.engine.builder;

import se.l4.silo.engine.SearchIndexQueryEngineFactory;
import se.l4.silo.engine.internal.search.SearchIndexQueryEngine;
import se.l4.silo.engine.search.Language;

/**
 * Builder for setting up shared information used by instances of
 * {@link SearchIndexQueryEngine}.
 * 
 * @author Andreas Holstenson
 *
 */
public interface SearchEngineBuilder
{
	/**
	 * Add a custom language implementation to this engine. This is for
	 * advanced usage with custom language implementations.
	 * 
	 * @param language
	 * @return
	 */
	SearchEngineBuilder addLanguage(Language language);
	
	/**
	 * Create the factory for {@link SearchIndexQueryEngine}s.
	 * 
	 * @return
	 */
	SearchIndexQueryEngineFactory build();
}
