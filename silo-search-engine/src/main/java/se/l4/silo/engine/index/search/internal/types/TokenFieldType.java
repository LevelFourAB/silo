package se.l4.silo.engine.index.search.internal.types;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import se.l4.silo.engine.index.search.SearchFieldDef;
import se.l4.silo.engine.index.search.query.QueryEncounter;
import se.l4.silo.engine.index.search.types.FieldCreationEncounter;
import se.l4.silo.index.EqualsMatcher;
import se.l4.silo.index.Matcher;
import se.l4.silo.index.search.SearchIndexException;

/**
 * Field type used to index a {@link String} as a token.
 */
public class TokenFieldType
	extends AbstractStringFieldType
{
	public static final TokenFieldType INSTANCE = new TokenFieldType();

	@Override
	public boolean isLocaleSupported()
	{
		return false;
	}

	@Override
	protected void createIndexed(FieldCreationEncounter<String> encounter)
	{
		encounter.emit(new StringField(
			encounter.name(),
			encounter.getValue(),
			Field.Store.NO
		));
	}

	@Override
	public Query createQuery(
		QueryEncounter<?> encounter,
		SearchFieldDef<?> fieldDef,
		Matcher<String> matcher
	)
	{
		String fieldName = encounter.index()
			.name(fieldDef, encounter.currentLanguage());

		if(matcher instanceof EqualsMatcher)
		{
			String value = ((EqualsMatcher<String>) matcher).getValue();
			return new TermQuery(new Term(fieldName, value.toString()));
		}

		throw new SearchIndexException("Token field queries require a " + EqualsMatcher.class.getName());
	}
}
