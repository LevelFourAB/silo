package se.l4.silo.engine.builder;

/**
 * Interface to mark that a builder has a parent that can be traversed to
 * with {@link #done()}.
 *
 * @author Andreas Holstenson
 *
 * @param <Parent>
 */
public interface BuilderWithParent<Parent>
{
	/**
	 * Finish building and return to the parent of this builder.
	 *
	 * @return
	 */
	Parent done();
}
