package se.l4.silo.engine.index.search.query;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;

import se.l4.silo.engine.index.search.SearchField;
import se.l4.silo.engine.index.search.SearchFieldDefinition;
import se.l4.silo.engine.index.search.SearchIndexEncounter;
import se.l4.silo.index.EqualsMatcher;
import se.l4.silo.index.Matcher;
import se.l4.silo.index.search.query.FieldQuery;

// TODO: Multiple language support
public class FieldQueryBuilder
	implements QueryBuilder<FieldQuery>
{
	@Override
	public Query parse(QueryEncounter<FieldQuery> encounter)
		throws IOException
	{
		FieldQuery clause = encounter.clause();

		SearchIndexEncounter<?> indexEncounter = encounter.index();
		SearchField<?, ?> field = indexEncounter.getField(clause.getField());
		SearchFieldDefinition<?> def = field.getDefinition();

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
				String fieldName = indexEncounter.nullName(def);
				return new TermQuery(new Term(fieldName, new BytesRef(BytesRef.EMPTY_BYTES)));
			}
		}

		// All other matchers are delegated to the field type
		String fieldName = indexEncounter.name(def, encounter.currentLanguage());
		return def.getType().createQuery(fieldName, matcher);
	}
}
