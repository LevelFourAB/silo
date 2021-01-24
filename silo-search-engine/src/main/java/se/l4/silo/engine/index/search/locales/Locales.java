package se.l4.silo.engine.index.search.locales;

import java.util.Optional;

public interface Locales
{
	/**
	 * Get the default instance.
	 *
	 * @return
	 */
	LocaleSupport getDefault();

	/**
	 * Get a {@link LocaleSupport} based on its identifier.
	 *
	 * @param id
	 *   identifier
	 * @return
	 *   instance that can be used within an index
	 */
	Optional<LocaleSupport> get(String id);

	/**
	 * Get a {@link LocaleSupport} or return the default instance.
	 *
	 * @param id
	 * @return
	 */
	LocaleSupport getOrDefault(String id);

	interface Builder
	{
		/**
		 * Set the default instance.
		 *
		 * @return
		 */
		Builder withDefault(LocaleSupport defaultInstance);

		/**
		 * Add a {@link LocaleSupport} that can be used.
		 *
		 * @param instance
		 * @return
		 */
		Builder add(LocaleSupport instance);

		/**
		 * Build this instance.
		 *
		 * @return
		 */
		Locales build();
	}
}
