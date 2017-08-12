package se.l4.silo.engine.search.internal;

import java.util.Locale;

import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;

import se.l4.silo.engine.search.AnalyzerField;
import se.l4.silo.engine.search.FieldDefinition;
import se.l4.silo.engine.search.Language;

/**
 * Abstract implementation of {@link FieldDefinition}.
 *
 * @author Andreas Holstenson
 *
 */
public abstract class AbstractFieldDefinition
	implements FieldDefinition
{
	public static String name(char p, String field, Locale language, boolean languageSpecific)
	{
		return (! languageSpecific || language == null) ? p + ":" + field + ":_" : p + ":" + field + ":" + language;
	}

	private String name(char p, String field, Locale language)
	{
		return name(p, field, language, isLanguageSpecific());
	}

	@Override
	public String name(Locale language)
	{
		return name(getName(), language);
	}

	@Override
	public String docValuesName(Locale language)
	{
		return name('v', getName(), language);
	}

	@Override
	public String sortValuesName(Locale language)
	{
		return name('s', getName(), language);
	}

	@Override
	public String name(String field, Language language)
	{
		return name('f', field, language == null ? null : language.getLocale());
	}

	@Override
	public String name(String field, Locale language)
	{
		return name('f', field, language);
	}

	@Override
	public String nullName(String field)
	{
		return name('n', field, null);
	}

	@Override
	public IndexableField createIndexableField(String name, Language language, Object data)
	{
		FieldType ft = createFieldType();

		ft.setStored(isStored());

		if(! isIndexed())
		{
			ft.setIndexOptions(IndexOptions.NONE);
		}
		else
		{
			ft.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
		}

		if(isHighlighted())
		{
			ft.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
		}

		// TODO: Set stored/indexed values
		String fieldName = name(name, language);
		IndexableField field = getType().create(fieldName, ft, language, data);
		if(ft.tokenized())
		{
			return new AnalyzerField(field, getType().getAnalyzer(language));
		}
		else
		{
			return field;
		}
	}

	@Override
	public IndexableField createValuesField(String name, Language language, Object data)
	{
		String fieldName = name('v', name, language == null ? null : language.getLocale());
		return getType().createValuesField(fieldName, language, data);
	}

	@Override
	public IndexableField createSortingField(String name, Language language, Object data)
	{
		String fieldName = name('s', name, language == null ? null : language.getLocale());
		return getType().createSortingField(fieldName, language, data);
	}

	private FieldType createFieldType()
	{
		FieldType ft = new FieldType();
		FieldType defaults = getType().getDefaultFieldType();
		ft.setIndexOptions(defaults.indexOptions());
		ft.setNumericPrecisionStep(defaults.numericPrecisionStep());
		ft.setNumericType(defaults.numericType());
		ft.setOmitNorms(defaults.omitNorms());
		ft.setStoreTermVectorOffsets(false);
		ft.setStoreTermVectorPositions(false);
		ft.setStoreTermVectors(false);
		ft.setTokenized(defaults.tokenized());
		return ft;
	}
}
