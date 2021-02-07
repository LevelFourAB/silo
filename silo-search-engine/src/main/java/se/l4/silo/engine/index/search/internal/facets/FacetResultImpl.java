package se.l4.silo.engine.index.search.internal.facets;

import java.util.Iterator;

import reactor.core.publisher.Flux;
import se.l4.silo.index.search.facets.FacetResult;
import se.l4.silo.index.search.facets.FacetValue;

/**
 * Implementation of {@link FacetResult}.
 */
public class FacetResultImpl<V>
	implements FacetResult<V>
{
	private final String id;
	private final Iterable<FacetValue<V>> values;

	public FacetResultImpl(
		String id,
		Iterable<FacetValue<V>> values
	)
	{
		this.id = id;
		this.values = values;
	}

	@Override
	public Iterator<FacetValue<V>> iterator()
	{
		return values.iterator();
	}

	@Override
	public String getId()
	{
		return id;
	}

	@Override
	public Flux<FacetValue<V>> asFlux()
	{
		return Flux.fromIterable(values);
	}

	@Override
	public String toString()
	{
		return "FacetResult{id=" + id + ", values=" + values + "}";
	}
}
