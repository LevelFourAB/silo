package se.l4.silo.engine.search.facets.date;

import java.util.Objects;
import java.util.function.Function;

import se.l4.silo.engine.builder.BuilderWithParent;
import se.l4.silo.engine.search.facets.Facet;
import se.l4.silo.engine.search.facets.category.CategoryFacet;

/**
 * Builder for instances of {@link CategoryFacet}.
 *
 * @author Andreas Holstenson
 *
 * @param <Parent>
 */
public class DateFacetBuilder<Parent>
	implements BuilderWithParent<Parent>
{
	private final Function<Facet<?>, Parent> instanceReceiver;
	private String field;

	public DateFacetBuilder(Function<Facet<?>, Parent> instanceReceiver)
	{
		this.instanceReceiver = instanceReceiver;
	}

	public DateFacetBuilder<Parent> setField(String field)
	{
		Objects.requireNonNull(field, "field must be given");
		this.field = field;
		return this;
	}

	@Override
	public Parent done()
	{
		Objects.requireNonNull(field, "field must be given");

		return instanceReceiver.apply(new DateFacet(field));
	}
}
