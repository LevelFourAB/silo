package se.l4.silo.engine.search;

import java.util.Locale;

import se.l4.silo.engine.QueryEngineFactory;
import se.l4.silo.engine.search.internal.SearchIndexQueryEngine;

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
	 * Set the default language for this engine.
	 * 
	 * @param locale
	 * @return
	 */
	SearchEngineBuilder setDefaultLanguage(Locale locale);
	
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
	QueryEngineFactory<?, ?> build();
}
