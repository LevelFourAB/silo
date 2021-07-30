package se.l4.silo.engine.index.search.internal;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.QueryBuilder;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.ImmutableSet;

import se.l4.silo.StorageException;
import se.l4.silo.engine.index.search.locales.LocaleSupport;
import se.l4.silo.engine.index.search.query.QueryEncounter;

public class UserQueryParser
{
	private final QueryEncounter<?> encounter;
	private boolean typeAhead;
	private ImmutableSet<String> fields;

	private UserQueryParser(
		QueryEncounter<?> encounter
	)
	{
		fields = Sets.immutable.empty();
		this.encounter = encounter;
	}

	public static UserQueryParser create(
		QueryEncounter<?> encounter
	)
	{
		return new UserQueryParser(encounter);
	}

	/**
	 * Set the fields to match.
	 *
	 * @param fields
	 * @return
	 */
	public UserQueryParser withFields(String... fields)
	{
		this.fields = Sets.immutable.of(fields);
		return this;
	}

	/**
	 * Set if this should be parsed as a type-ahead query.
	 *
	 * @param x
	 * @return
	 */
	public UserQueryParser withTypeAhead(boolean typeAhead)
	{
		this.typeAhead = typeAhead;
		return this;
	}

	public Query parse(
		String q
	)
	{
		return parse(encounter.currentLanguage(), q);
	}

	private Query parse(LocaleSupport locale, String query)
	{
		QueryBuilder builder = new QueryBuilder(
			typeAhead ? locale.getTypeAheadAnalyzer() : locale.getTextAnalyzer()
		);

		boolean inQuote = false;
		BooleanQuery.Builder result = new BooleanQuery.Builder();
		int last = 0;
		for(int i=0, n=query.length(); i<n; i++)
		{
			char c = query.charAt(i);
			if(c == '"')
			{
				if(inQuote)
				{
					Query fq = getPhraseQuery(
						builder,
						query.substring(last, i)
					);

					if(fq != null)
					{
						result.add(fq, BooleanClause.Occur.MUST);
					}
				}
				else if(last < i-1)
				{
					Query fq = getBooleanQuery(
						builder,
						query.substring(last, i)
					);

					if(fq != null)
					{
						result.add(fq, BooleanClause.Occur.MUST);
					}
				}

				last = i+1;
				inQuote = ! inQuote;
			}
		}

		if(inQuote)
		{
			Query fq = getPhraseQuery(
				builder,
				query.substring(last, query.length())
			);

			if(fq != null)
			{
				result.add(fq, BooleanClause.Occur.MUST);
			}
		}
		else if(last < query.length())
		{
			Query fq = getBooleanQuery(
				builder,
				query.substring(last, query.length())
			);

			if(fq != null)
			{
				result.add(fq, BooleanClause.Occur.MUST);
			}
		}

		return result.build();
	}

	private Query getBooleanQuery(
		QueryBuilder builder,
		String q
	)
	{
		if(fields.size() == 1)
		{
			return builder.createBooleanQuery(fields.getAny(), q, BooleanClause.Occur.MUST);
		}

		throw new StorageException("No support for querying multiple fields");
	}

	private Query getPhraseQuery(
		QueryBuilder builder,
		String q
	)
	{
		if(fields.size() == 1)
		{
			return builder.createPhraseQuery(fields.getAny(), q);
		}

		throw new StorageException("No support for querying multiple fields");
	}
}
