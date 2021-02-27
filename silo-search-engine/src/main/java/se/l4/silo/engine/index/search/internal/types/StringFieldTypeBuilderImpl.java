package se.l4.silo.engine.index.search.internal.types;

import se.l4.silo.engine.index.search.types.StringFieldType;
import se.l4.silo.engine.index.search.types.StringFieldType.FullTextBuilder;
import se.l4.silo.engine.index.search.types.StringFieldType.TokenBuilder;

/**
 * Implementation of {@link StringFieldType}.
 */
public class StringFieldTypeBuilderImpl
	implements StringFieldType.Builder
{
	public StringFieldTypeBuilderImpl()
	{
	}

	@Override
	public TokenBuilder token()
	{
		return new TokenBuilderImpl();
	}

	@Override
	public FullTextBuilder fullText()
	{
		return new FullTextBuilderImpl(false);
	}

	private static class TokenBuilderImpl
		implements TokenBuilder
	{
		@Override
		public StringFieldType build()
		{
			return TokenFieldType.INSTANCE;
		}
	}

	private static class FullTextBuilderImpl
		implements FullTextBuilder
	{
		private final boolean typeAhead;

		public FullTextBuilderImpl(boolean typeAhead)
		{
			this.typeAhead = typeAhead;
		}

		@Override
		public FullTextBuilder withTypeAhead()
		{
			return withTypeAhead(true);
		}

		@Override
		public FullTextBuilder withTypeAhead(boolean typeAhead)
		{
			return new FullTextBuilderImpl(typeAhead);
		}

		@Override
		public StringFieldType build()
		{
			if(typeAhead)
			{
				return FullTextFieldType.WITH_TYPE_AHEAD;
			}
			else
			{
				return FullTextFieldType.WITH_TYPE_AHEAD;
			}
		}
	}
}
