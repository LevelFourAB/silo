package se.l4.silo.engine.index.search.query;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import se.l4.silo.engine.index.search.SearchField;
import se.l4.silo.engine.index.search.SearchFieldDef;
import se.l4.silo.engine.index.search.SearchIndexEncounter;
import se.l4.silo.engine.index.search.internal.NullFields;
import se.l4.silo.index.AnyMatcher;
import se.l4.silo.index.EqualsMatcher;
import se.l4.silo.index.Matcher;
import se.l4.silo.index.NullMatcher;
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
		SearchFieldDef<?> def = field.getDefinition();

		Matcher matcher = clause.getMatcher();
		if(matcher instanceof NullMatcher
			|| (matcher instanceof EqualsMatcher && ((EqualsMatcher) matcher).getValue() == null))
		{
			/*
			 * Handle the case of null being matched.
			 */
			String fieldName = indexEncounter.nullName(def);
			return new TermQuery(new Term(fieldName, NullFields.VALUE_NULL));
		}
		else if(matcher instanceof AnyMatcher)
		{
			/**
			 * Handle the case of matching any value.
			 */
			String fieldName = indexEncounter.nullName(def);
			return new TermQuery(new Term(fieldName, NullFields.VALUE_NON_NULL));
		}

		// All other matchers are delegated to the field type
		String fieldName = indexEncounter.name(def, encounter.currentLanguage());
		return def.getType().createQuery(fieldName, matcher);
	}
}
