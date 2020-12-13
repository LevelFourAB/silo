package se.l4.silo.search.query;

/**
 * Matcher that looks for a specific phrase in a text field.
 */
public interface ContainsMatcher
{
	/**
	 * Get the phrase to match against.
	 *
	 * @return
	 */
	String getPhrase();
}
