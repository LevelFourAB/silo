package se.l4.silo.engine.index.search.internal;

import java.util.Optional;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;

import se.l4.silo.engine.index.search.query.AndQueryBuilder;
import se.l4.silo.engine.index.search.query.FieldQueryBuilder;
import se.l4.silo.engine.index.search.query.OrQueryBuilder;
import se.l4.silo.engine.index.search.query.QueryBuilder;
import se.l4.silo.engine.index.search.query.QueryBuilders;
import se.l4.silo.index.search.QueryClause;
import se.l4.silo.index.search.SearchIndexException;
import se.l4.ylem.types.matching.ClassMatchingMap;
import se.l4.ylem.types.matching.ImmutableClassMatchingMap;
import se.l4.ylem.types.reflect.TypeRef;
import se.l4.ylem.types.reflect.Types;

public class QueryBuildersImpl
	implements QueryBuilders
{
	public static final Builder DEFAULT_BUILDER = new BuilderImpl(Maps.immutable.empty())
		.add(new AndQueryBuilder())
		.add(new OrQueryBuilder())
		.add(new FieldQueryBuilder());

	public static final QueryBuilders DEFAULT = DEFAULT_BUILDER.build();

	private final ClassMatchingMap<QueryClause, QueryBuilder<?>> parsers;

	public QueryBuildersImpl(
		ClassMatchingMap<QueryClause, QueryBuilder<?>> parsers
	)
	{
		this.parsers = parsers.toImmutable();
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <C extends QueryClause> Optional<QueryBuilder<C>> get(Class<C> clause)
	{
		return (Optional) parsers.getBest(clause);
	}

	public static class BuilderImpl
		implements Builder
	{
		private final ImmutableMap<Class<? extends QueryClause>, QueryBuilder<?>> parsers;

		public BuilderImpl(
			ImmutableMap<Class<? extends QueryClause>, QueryBuilder<?>> parsers
		)
		{
			this.parsers = parsers;
		}

		@Override
		public <C extends QueryClause> Builder add(
			Class<C> clause,
			QueryBuilder<C> parser
		)
		{
			return new BuilderImpl(
				parsers.newWithKeyValue(clause, parser)
			);
		}

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public Builder add(QueryBuilder<? extends QueryClause> parser)
		{
			TypeRef param = Types.reference(parser.getClass())
				.findInterface(QueryBuilder.class)
				.flatMap(ref -> ref.getTypeParameter(0))
				.orElseThrow(() -> new SearchIndexException("Unable to use reflection to find QueryClause handled by " + parser.getClass()));

			if(! QueryClause.class.isAssignableFrom(param.getErasedType()))
			{
				throw new SearchIndexException(
					"Unable to use reflection to find QueryClause handled by "
					+ parser.getClass() + "; Parameter of QueryParser does not "
					+ "implement QueryClause, got: " + param.getErasedType()
				);
			}

			return add((Class) param.getErasedType(), parser);
		}

		@Override
		public QueryBuilders build()
		{
			return new QueryBuildersImpl(
				new ImmutableClassMatchingMap<>(parsers)
			);
		}
	}
}
