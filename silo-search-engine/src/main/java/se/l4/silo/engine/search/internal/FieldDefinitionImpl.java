package se.l4.silo.engine.search.internal;

import se.l4.silo.engine.search.FieldDefinition;
import se.l4.silo.engine.search.SearchFieldType;

public class FieldDefinitionImpl
	extends AbstractFieldDefinition
{
	private String name;
	private boolean isLanguageSpecific;
	private boolean isIndexed;
	private boolean isStored;
	private boolean isHighlighted;
	private boolean isSorted;
	private boolean isStoreValues;
	private SearchFieldType type;

	public FieldDefinitionImpl(String name,
			SearchFieldType type,
			boolean isLanguageSpecific,
			boolean isIndexed,
			boolean isStored,
			boolean isHighlighted,
			boolean isSorted,
			boolean isStoreValues)
	{
		this.name = name;
		this.type = type;
		this.isLanguageSpecific = isLanguageSpecific;
		this.isIndexed = isIndexed;
		this.isStored = isStored;
		this.isHighlighted = isHighlighted;
		this.isSorted = isSorted;
		this.isStoreValues = isStoreValues;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public boolean isLanguageSpecific()
	{
		return isLanguageSpecific;
	}

	@Override
	public boolean isIndexed()
	{
		return isIndexed;
	}

	@Override
	public boolean isStored()
	{
		return isStored;
	}

	@Override
	public boolean isHighlighted()
	{
		return isHighlighted;
	}

	@Override
	public boolean isSorted()
	{
		return isSorted;
	}

	@Override
	public boolean isStoreValues()
	{
		return isStoreValues;
	}

	@Override
	public SearchFieldType getType()
	{
		return type;
	}

	public static class BuilderImpl
		implements Builder
	{
		private String name;
		private SearchFieldType type;
		private boolean languageSpecific;
		private boolean indexed;
		private boolean stored;
		private boolean highlighted;
		private boolean sorted;
		private boolean storeValues;

		@Override
		public Builder setName(String name)
		{
			this.name = name;
			return this;
		}

		@Override
		public Builder setType(SearchFieldType type)
		{
			this.type = type;
			return this;
		}

		@Override
		public Builder setLanguageSpecific(boolean languageSpecific)
		{
			this.languageSpecific = languageSpecific;
			return this;
		}

		@Override
		public FieldDefinition build()
		{
			return new FieldDefinitionImpl(name, type, languageSpecific, indexed, stored, highlighted, sorted, storeValues);
		}

	}
}
