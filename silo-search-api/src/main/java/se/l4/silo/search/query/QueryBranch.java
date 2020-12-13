package se.l4.silo.search.query;

import java.util.Arrays;

import se.l4.silo.query.Matcher;
import se.l4.silo.search.QueryClause;
import se.l4.silo.search.internal.AndQueryImpl;
import se.l4.silo.search.internal.OrQueryImpl;

/**
 * Marker interface for {@link QueryClause}s that create branches.
 */
public interface QueryBranch
	extends QueryClause
{
	interface Builder<B extends Builder<B>>
	{
		/**
		 * Add one or more pre-built {@link QueryClause}s.
		 *
		 * @param clauses
		 * @return
		 *   new builder with the given clause added
		 */
		default B add(QueryClause... clauses)
		{
			return add(Arrays.asList(clauses));
		}

		/**
		 * Add several pre-built {@link QueryClause}s.
		 *
		 * @param clauses
		 * @return
		 *   new builder with the given clauses
		 */
		B add(Iterable<? extends QueryClause> clauses);

		/**
		 * Add a {@link AndQuery} containing the given clauses, meaning all of
		 * them must match.
		 *
		 * @param clauses
		 * @return
		 */
		default B and(QueryClause... clauses)
		{
			return and(Arrays.asList(clauses));
		}

		/**
		 * Add a {@link AndQuery} containing the given clauses, meaning all of
		 * them must match.
		 *
		 * @param clauses
		 * @return
		 */
		default B and(Iterable<? extends QueryClause> clauses)
		{
			return add(new AndQueryImpl(clauses));
		}

		/**
		 * Add a {@link OrQuery} containing the given clauses, meaning at least
		 * one of them must match.
		 *
		 * @param clauses
		 * @return
		 */
		default B or(QueryClause... clauses)
		{
			return or(Arrays.asList(clauses));
		}

		/**
		 * Add a {@link OrQuery} containing the given clauses, meaning at least
		 * one of them must match.
		 *
		 * @param clauses
		 * @return
		 */
		default B or(Iterable<? extends QueryClause> clauses)
		{
			return add(new OrQueryImpl(clauses));
		}

		/**
		 * Add a clause that may never match to this branch. Creates a
		 * {@link NegateQuery}.
		 *
		 * @param clause
		 * @return
		 */
		default B not(QueryClause clause)
		{
			return add(NegateQuery.create(clause));
		}

		/**
		 * Limit the given field in a fluent way.
		 *
		 * @param name
		 *   the name of the field
		 * @return
		 *   builder that can be used to define how the field is to be
		 *   matched
		 */
		default SearchIndexLimitBuilder<B, Object> field(String name)
		{
			return matcher -> field(name, matcher);
		}

		/**
		 * Limit the given field using a pre-built matcher.
		 *
		 * @param name
		 *   the name of the field
		 * @param matcher
		 *   the matcher that should be used to limit the field. Created
		 *   via static methods in {@link Matcher}
		 * @return
		 *   copy of builder with field limit added
		 */
		default B field(String name, Matcher matcher)
		{
			return add(FieldQuery.create(name, matcher));
		}
	}
}
