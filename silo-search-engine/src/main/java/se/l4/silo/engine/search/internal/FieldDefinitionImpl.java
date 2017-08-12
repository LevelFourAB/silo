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

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[name=" + name + ", isLanguageSpecific=" + isLanguageSpecific + ", isIndexed="
			+ isIndexed + ", isStored=" + isStored + ", isHighlighted=" + isHighlighted + ", isSorted=" + isSorted
			+ ", isStoreValues=" + isStoreValues + ", type=" + type + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (isHighlighted ? 1231 : 1237);
		result = prime * result + (isIndexed ? 1231 : 1237);
		result = prime * result + (isLanguageSpecific ? 1231 : 1237);
		result = prime * result + (isSorted ? 1231 : 1237);
		result = prime * result + (isStoreValues ? 1231 : 1237);
		result = prime * result + (isStored ? 1231 : 1237);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		FieldDefinitionImpl other = (FieldDefinitionImpl) obj;
		if(isHighlighted != other.isHighlighted)
			return false;
		if(isIndexed != other.isIndexed)
			return false;
		if(isLanguageSpecific != other.isLanguageSpecific)
			return false;
		if(isSorted != other.isSorted)
			return false;
		if(isStoreValues != other.isStoreValues)
			return false;
		if(isStored != other.isStored)
			return false;
		if(name == null)
		{
			if(other.name != null)
				return false;
		}
		else if(!name.equals(other.name))
			return false;
		if(type == null)
		{
			if(other.type != null)
				return false;
		}
		else if(!type.equals(other.type))
			return false;
		return true;
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
			languageSpecific = type.isLanguageSpecific();
			return this;
		}

		@Override
		public Builder setLanguageSpecific(boolean languageSpecific)
		{
			this.languageSpecific = languageSpecific;
			return this;
		}

		@Override
		public Builder setSorted(boolean sorted)
		{
			this.sorted = sorted;
			return this;
		}

		@Override
		public FieldDefinition build()
		{
			return new FieldDefinitionImpl(name, type, languageSpecific, indexed, stored, highlighted, sorted, storeValues);
		}

	}
}
