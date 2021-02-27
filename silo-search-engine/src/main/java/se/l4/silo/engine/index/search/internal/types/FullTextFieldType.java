package se.l4.silo.engine.index.search.internal.types;

import java.io.IOException;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;

import se.l4.exobytes.streaming.StreamingInput;
import se.l4.exobytes.streaming.StreamingOutput;
import se.l4.exobytes.streaming.Token;
import se.l4.silo.engine.index.search.types.AnalyzingTextField;
import se.l4.silo.engine.index.search.types.FieldCreationEncounter;
import se.l4.silo.engine.index.search.types.StringFieldType;
import se.l4.silo.index.EqualsMatcher;
import se.l4.silo.index.Matcher;
import se.l4.silo.index.search.SearchIndexException;

public class FullTextFieldType
	implements StringFieldType
{
	public static final StringFieldType WITHOUT_TYPE_AHEAD = new FullTextFieldType(false);
	public static final StringFieldType WITH_TYPE_AHEAD = new FullTextFieldType(true);

	private final boolean typeAhead;

	private FullTextFieldType(boolean typeAhead)
	{
		this.typeAhead = typeAhead;
	}

	@Override
	public String read(StreamingInput in)
		throws IOException
	{
		in.next(Token.VALUE);
		return in.readString();
	}

	@Override
	public void write(String instance, StreamingOutput out)
		throws IOException
	{
		out.writeString(instance);
	}

	@Override
	public boolean isLocaleSupported()
	{
		return true;
	}

	@Override
	public boolean isSortingSupported()
	{
		return true;
	}

	@Override
	public boolean isDocValuesSupported()
	{
		return true;
	}

	@Override
	public void create(FieldCreationEncounter<String> encounter)
	{
		if(encounter.isIndexed())
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

		if(encounter.isSorted())
		{
			encounter.emit(new SortedDocValuesField(
				encounter.sortValuesName(),
				new BytesRef(encounter.getValue())
			));
		}

		if(encounter.isStoreDocValues())
		{
			encounter.emit(new SortedSetDocValuesField(
				encounter.docValuesName(),
				new BytesRef(encounter.getValue())
			));
		}
	}

	@Override
	public SortField createSortField(String field, boolean ascending)
	{
		return new SortField(field, SortField.Type.STRING, ! ascending);
	}

	@Override
	public Query createQuery(String field, Matcher<String> matcher)
	{
		if(matcher instanceof EqualsMatcher)
		{
			String value = ((EqualsMatcher<String>) matcher).getValue();
			return new TermQuery(new Term(field, value.toString()));
		}

		throw new SearchIndexException("Token field queries require a " + EqualsMatcher.class.getName());
	}
}
