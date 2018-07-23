package se.l4.silo.engine.search.facets.category;

import java.util.Objects;
import java.util.function.Function;

import se.l4.silo.engine.builder.BuilderWithParent;
import se.l4.silo.engine.search.facets.Facet;

/**
 * Builder for instances of {@link CategoryFacet}.
 *
 * @author Andreas Holstenson
 *
 * @param <Parent>
 */
public class CategoryFacetBuilder<Parent>
	implements BuilderWithParent<Parent>
{
	private final Function<Facet<?>, Parent> instanceReceiver;
	private String field;

	public CategoryFacetBuilder(Function<Facet<?>, Parent> instanceReceiver)
	{
		this.instanceReceiver = instanceReceiver;
	}

	public CategoryFacetBuilder<Parent> setField(String field)
	{
		Objects.requireNonNull(field, "field must be given");
		this.field = field;
		return this;
	}

	@Override
	public Parent done()
	{
		Objects.requireNonNull(field, "field must be given");

		return instanceReceiver.apply(new CategoryFacet(field));
	}
}
