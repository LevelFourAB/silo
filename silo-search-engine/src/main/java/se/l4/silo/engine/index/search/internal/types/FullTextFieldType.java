package se.l4.silo.engine.index.search.internal.types;

import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import se.l4.silo.engine.index.search.internal.UserQueryParser;
import se.l4.silo.engine.index.search.query.QueryEncounter;
import se.l4.silo.engine.index.search.types.AnalyzingTextField;
import se.l4.silo.engine.index.search.types.FieldCreationEncounter;
import se.l4.silo.engine.index.search.types.StringFieldType;
import se.l4.silo.index.EqualsMatcher;
import se.l4.silo.index.Matcher;
import se.l4.silo.index.search.SearchIndexException;
import se.l4.silo.index.search.query.UserQuery;
import se.l4.silo.index.search.query.UserQueryMatcher;

public class FullTextFieldType
	extends AbstractStringFieldType
{
	public static final StringFieldType WITHOUT_TYPE_AHEAD = new FullTextFieldType(false);
	public static final StringFieldType WITH_TYPE_AHEAD = new FullTextFieldType(true);

	private final boolean typeAhead;

	private FullTextFieldType(boolean typeAhead)
	{
		this.typeAhead = typeAhead;
	}

	@Override
	public boolean isLocaleSupported()
	{
		return true;
	}

	@Override
	protected void createIndexed(FieldCreationEncounter<String> encounter)
	{
		encounter.emit(new AnalyzingTextField(
			encounter.name(),
			encounter.getValue(),
			Field.Store.NO,
			encounter.getLocale().getTextAnalyzer()
		));

		if(typeAhead)
		{
			encounter.emit(new AnalyzingTextField(
				encounter.name("type-ahead"),
				encounter.getValue(),
				Field.Store.NO,
				encounter.getLocale().getTypeAheadAnalyzer()
			));
		}
	}

	@Override
	public Query createQuery(
		QueryEncounter<?> encounter,
		String field,
		Matcher<String> matcher
	)
	{
		if(matcher instanceof EqualsMatcher)
		{
			String value = ((EqualsMatcher<String>) matcher).getValue();
			return new TermQuery(new Term(field, value.toString()));
		}
		else if(matcher instanceof UserQueryMatcher)
		{
			UserQueryMatcher userQuery = (UserQueryMatcher) matcher;
			return UserQueryParser.create(encounter)
				.withFields(field)
				.withTypeAhead(userQuery.getContext() == UserQuery.Context.TYPE_AHEAD)
				.parse(userQuery.getQuery());
		}

		throw new SearchIndexException("Token field queries require a " + EqualsMatcher.class.getName());
	}
}
