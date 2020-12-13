package se.l4.silo.engine.search.query;

import java.io.IOException;

import org.apache.lucene.search.Query;

import se.l4.silo.search.QueryClause;

/**
 * Interface for pluggable query creation from a {@link QueryClause}.
 */
public interface QueryBuilder<QC extends QueryClause>
{
	/**
	 * Process a {@link QueryClause} into a Lucene {@link Query}.
	 *
	 * @param encounter
	 * @return
	 * @throws IOException
	 */
	Query parse(QueryEncounter<QC> encounter)
		throws IOException;
}
