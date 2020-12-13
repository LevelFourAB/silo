package se.l4.silo.engine.search.query;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;

import se.l4.silo.engine.search.SearchFieldDefinition;
import se.l4.silo.engine.search.SearchIndexEncounter;
import se.l4.silo.query.EqualsMatcher;
import se.l4.silo.query.Matcher;
import se.l4.silo.search.query.FieldQuery;

// TODO: Multiple language support
public class FieldQueryBuilder
	implements QueryBuilder<FieldQuery>
{
	@Override
	public Query parse(QueryEncounter<FieldQuery> encounter)
		throws IOException
	{
		FieldQuery clause = encounter.clause();

		SearchIndexEncounter indexEncounter = encounter.index();
		SearchFieldDefinition<?> field = indexEncounter.getField(clause.getField());

		Matcher matcher = clause.getMatcher();
		if(matcher instanceof EqualsMatcher)
		{
			/*
			 * Handle the special case of null being matched. This is stored
			 * in an alternative field name.
			 */
			EqualsMatcher equals = (EqualsMatcher) matcher;
			if(equals.getValue() == null)
			{
				String fieldName = indexEncounter.nullName(field);
				return new TermQuery(new Term(fieldName, new BytesRef(BytesRef.EMPTY_BYTES)));
			}
		}

		// All other matchers are delegated to the field type
		String fieldName = indexEncounter.name(field, encounter.currentLanguage());
		return field.getType().createQuery(fieldName, matcher);
	}
}
