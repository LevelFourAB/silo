package se.l4.silo.engine.index.search.types;

import java.io.IOException;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.BytesRef;

import se.l4.exobytes.streaming.StreamingInput;
import se.l4.exobytes.streaming.StreamingOutput;
import se.l4.exobytes.streaming.Token;
import se.l4.silo.index.Matcher;
import se.l4.silo.index.search.SearchIndexException;
import se.l4.silo.index.search.query.UserQuery;

public class TextFieldType
	implements SearchFieldType<String>
{
	private final boolean suggest;

	public TextFieldType(
		boolean suggest
	)
	{
		this.suggest = suggest;
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
				encounter.isHighlighted() ? Field.Store.YES : Field.Store.NO,
				encounter.getLocale().getTextAnalyzer()
			));
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
	public Query createQuery(String field, Matcher matcher)
	{
		if(matcher instanceof UserQuery.Matcher)
		{

		}
		throw new SearchIndexException("Text field queries do not support any matchers");
	}
}
