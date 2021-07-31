package se.l4.silo.engine.index.search.internal.query;

import java.io.IOException;

import org.apache.lucene.search.Query;

import se.l4.silo.engine.index.search.internal.UserQueryParser;
import se.l4.silo.engine.index.search.query.QueryBuilder;
import se.l4.silo.engine.index.search.query.QueryEncounter;
import se.l4.silo.index.search.query.UserQuery;

/**
 * Builder that handles {@link UserQuery}.
 */
public class UserQueryBuilder
	implements QueryBuilder<UserQuery>
{
	@Override
	public Query parse(QueryEncounter<UserQuery> encounter)
		throws IOException
	{
		UserQuery q = encounter.clause();
		return UserQueryParser.create(encounter)
			.addFields(q.getFields(), q.getBoosts())
			.withTypeAhead(q.getContext() == UserQuery.Context.TYPE_AHEAD)
			.parse(q.getQuery());
	}
}
