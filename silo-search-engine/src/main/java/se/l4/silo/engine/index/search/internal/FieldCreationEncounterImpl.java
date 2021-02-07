package se.l4.silo.engine.index.search.internal;

import java.util.function.Consumer;

import org.apache.lucene.index.IndexableField;

import se.l4.silo.engine.index.search.SearchField;
import se.l4.silo.engine.index.search.SearchFieldDefinition;
import se.l4.silo.engine.index.search.SearchIndexEncounter;
import se.l4.silo.engine.index.search.locales.LocaleSupport;
import se.l4.silo.engine.index.search.types.FieldCreationEncounter;

public class FieldCreationEncounterImpl<T>
	implements FieldCreationEncounter<T>
{
	private final SearchIndexEncounter<?> index;
	private final Consumer<IndexableField> fieldReceiver;
	private final SearchField<?, T> field;
	private final LocaleSupport locale;
	private final T value;

	public FieldCreationEncounterImpl(
		SearchIndexEncounter<?> index,
		Consumer<IndexableField> fieldReceiver,
		SearchField<?, T> field,
		LocaleSupport locale,
		T value
	)
	{
		this.index = index;
		this.fieldReceiver = fieldReceiver;
		this.field = field;
		this.locale = locale;
		this.value = value;
	}

	@Override
	public T getValue()
	{
		return value;
	}

	@Override
	public boolean isIndexed()
	{
		return field.isIndexed();
	}

	@Override
	public boolean isStored()
	{
		return field.getDefinition().isHighlighted();
	}

	@Override
	public boolean isHighlighted()
	{
		return field.getDefinition().isHighlighted();
	}

	@Override
	@SuppressWarnings({ "rawtypes" })
	public boolean isSorted()
	{
		return field instanceof SearchFieldDefinition.Single
			&& ((SearchFieldDefinition.Single) field).isSorted();
	}

	@Override
	public boolean isStoreDocValues()
	{
		return field.isStoreDocValues();
	}

	@Override
	public LocaleSupport getLocale()
	{
		return locale;
	}

	@Override
	public String docValuesName()
	{
		return index.docValuesName(field.getDefinition(), locale);
	}

	@Override
	public String sortValuesName()
	{
		return index.sortValuesName(field.getDefinition(), locale);
	}

	@Override
	public String name()
	{
		return index.name(field.getDefinition(), locale);
	}

	@Override
	public String name(String variant)
	{
		return name() + ":" + variant;
	}

	@Override
	public void emit(IndexableField field)
	{
		fieldReceiver.accept(field);
	}
}
