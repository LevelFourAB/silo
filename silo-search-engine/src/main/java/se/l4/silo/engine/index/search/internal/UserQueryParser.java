package se.l4.silo.engine.index.search.internal;

import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.QueryBuilder;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.FloatList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;

import se.l4.silo.engine.index.search.SearchFieldDef;
import se.l4.silo.engine.index.search.internal.types.FullTextFieldType;
import se.l4.silo.engine.index.search.locales.LocaleSupport;
import se.l4.silo.engine.index.search.query.QueryEncounter;
import se.l4.silo.engine.index.search.types.SearchFieldType;

/**
 * Parser that takes user-style queries and turns them into {@link Query}
 * objects.
 */
public class UserQueryParser
{
	private final QueryEncounter<?> encounter;
	private final MutableList<FieldWithBoost> fields;
	private boolean typeAhead;
	private BooleanClause.Occur operator;

	private UserQueryParser(
		QueryEncounter<?> encounter
	)
	{
		fields = Lists.mutable.empty();
		this.encounter = encounter;

		operator = BooleanClause.Occur.MUST;
	}

	/**
	 * Create a new instance on top of the given query encounter.
	 *
	 * @param encounter
	 * @return
	 */
	public static UserQueryParser create(
		QueryEncounter<?> encounter
	)
	{
		return new UserQueryParser(encounter);
	}

	/**
	 * Add a field that should be queried.
	 *
	 * @param fields
	 * @return
	 */
	public UserQueryParser addField(SearchFieldDef<?> def)
	{
		return addField(def, 1.0f);
	}

	/**
	 * Add a field that should be queried with an optional boost.
	 *
	 * @param fields
	 * @return
	 */
	public UserQueryParser addField(SearchFieldDef<?> def, float boost)
	{
		this.fields.add(new FieldWithBoost(def, boost));
		return this;
	}

	/**
	 * Add multiple fields with boosts.
	 *
	 * @param fields
	 * @param boosts
	 * @return
	 */
	public UserQueryParser addFields(ListIterable<String> fields, FloatList boosts)
	{
		for(int i=0, n=fields.size(); i<n; i++)
		{
			this.fields.add(new FieldWithBoost(
				encounter.index().getField(fields.get(i)).getDefinition(),
				boosts.size() > i ? boosts.get(i) : 1.0f
			));
		}

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

	/**
	 * Set if the query should allow terms to be optional, resulting in more
	 * matches.
	 *
	 * @param optional
	 * @return
	 */
	public UserQueryParser withOptionalTerms(boolean optional)
	{
		this.operator = optional ? BooleanClause.Occur.SHOULD : BooleanClause.Occur.MUST;
		return this;
	}

	/**
	 * Parse the given query.
	 *
	 * @param query
	 * @return
	 */
	public Query parse(String query)
	{
		if(encounter.currentLanguage() != encounter.defaultLanguage()
			&& hasLanguageSpecificFields())
		{
			/*
			 * When a specific query language is used we parse for both
			 * languages and let the highest scoring query win.
			 */
			return new DisjunctionMaxQuery(List.of(
				parse(encounter.currentLanguage(), query),
				parse(encounter.defaultLanguage(), query)
			), 0.0f);
		}
		else
		{
			// Same language, parse only one time
			return parse(encounter.currentLanguage(), query);
		}
	}

	/**
	 * Get if any fields are language specific.
	 *
	 * @return
	 */
	private boolean hasLanguageSpecificFields()
	{
		for(FieldWithBoost f : fields)
		{
			if(f.def.isLanguageSpecific()) return true;
		}

		return false;
	}

	/**
	 * Perform a parse for the given locale.
	 *
	 * Parsing and matching a query over several fields in a way that fits
	 * user expectations requires some trickery.
	 *
	 * If the same analyzer is used for all fields we can combine them in
	 * a way where all terms must match, but they can be located in any
	 * field. This allows for searches through say a title and body field
	 * for two words and title matching one of the words and the body matching
	 * the other.
	 *
	 * If the analyzer for fields differ this isn't as straight forward so
	 * we parse things with a SHOULD operator instead. This allows terms
	 * to occur in any field, but we can't guarantee that all the terms
	 * have been found.
	 *
	 * @param locale
	 * @param query
	 * @return
	 */
	private Query parse(
		LocaleSupport locale,
		String query
	)
	{
		ListIterable<Pair<FieldWithBoost, Analyzer>> fieldsWithAnalyzers =
			fields.collect(field -> {
				if(typeAhead && isTypeAhead(field.def.getType()))
				{
					return Tuples.pair(field, locale.getTypeAheadAnalyzer());
				}
				else
				{
					return Tuples.pair(field, locale.getTextAnalyzer());
				}
			});

		Analyzer firstAnalyzer = fieldsWithAnalyzers.get(0).getTwo();
		if(fieldsWithAnalyzers.allSatisfy(pair -> pair.getTwo() == firstAnalyzer))
		{
			/*
			 * If all the analyzers are all the same we parse with all of the
			 * fields.
			 */
			return parse(locale, fields, firstAnalyzer, operator, query);
		}

		/*
		 * Analyzers are different parse every single field on its own and
		 * create a BooleanQuery for them.
		 */
		BooleanQuery.Builder result = new BooleanQuery.Builder();
		for(Pair<FieldWithBoost, Analyzer> pair : fieldsWithAnalyzers)
		{
			result.add(
				parse(locale, Lists.immutable.of(pair.getOne()), pair.getTwo(), BooleanClause.Occur.SHOULD, query),
				BooleanClause.Occur.SHOULD
			);
		}

		return result.build();
	}

	private boolean isTypeAhead(SearchFieldType<?> fieldType)
	{
		return fieldType instanceof FullTextFieldType
			&& ((FullTextFieldType) fieldType).isTypeAhead();
	}

	/**
	 * Parse a query for the given locale. This implements the overall query
	 * syntax such as support for quotes.
	 *
	 * @param locale
	 *   locale to parse for
	 * @param query
	 *   query to parse
	 * @return
	 *   parsed query
	 */
	private Query parse(
		LocaleSupport locale,
		ListIterable<FieldWithBoost> fields,
		Analyzer analyzer,
		BooleanClause.Occur operator,
		String query
	)
	{
		QueryBuilder builder = new QueryBuilder(analyzer);

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
						locale,
						fields,
						builder,
						query.substring(last, i)
					);

					if(fq != null)
					{
						result.add(fq, operator);
					}
				}
				else if(last < i-1)
				{
					Query fq = getBooleanQuery(
						locale,
						fields,
						builder,
						query.substring(last, i),
						operator
					);

					if(fq != null)
					{
						result.add(fq, operator);
					}
				}

				last = i+1;
				inQuote = ! inQuote;
			}
		}

		if(inQuote)
		{
			Query fq = getPhraseQuery(
				locale,
				fields,
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
				locale,
				fields,
				builder,
				query.substring(last, query.length()),
				operator
			);

			if(fq != null)
			{
				result.add(fq, BooleanClause.Occur.MUST);
			}
		}

		return result.build();
	}

	/**
	 * Get a boolean query for a sub-query.
	 *
	 * @param builder
	 *   builder to use to parse
	 * @param query
	 *   sub-query to parse
	 * @param operator
	 *   operator to use for the resulting query
	 * @return
	 */
	private Query getBooleanQuery(
		LocaleSupport locale,
		ListIterable<FieldWithBoost> fields,
		QueryBuilder builder,
		String query,
		BooleanClause.Occur operator
	)
	{
		if(fields.size() == 1)
		{
			FieldWithBoost field = fields.getAny();
			String fieldName = fieldName(locale, field.def);
			Query subQuery = builder.createBooleanQuery(fieldName, query, operator);
			return field.maybeBoost(subQuery);
		}
		else
		{
			/*
			 * Parse the query for each field and then try to stich them
			 * together using DisjunctionMaxQuery.
			 */
			MutableList<Query> queries = fields.collect(field -> {
				String fieldName = fieldName(locale, field.def);
				return builder.createBooleanQuery(fieldName, query, operator);
			}).toList();

			if(queries.getFirst() instanceof BooleanQuery)
			{
				BooleanQuery.Builder result = new BooleanQuery.Builder();

				BooleanQuery q0 = (BooleanQuery) queries.getFirst();
				for(int i=0, n=q0.clauses().size(); i<n; i++)
				{
					MutableList<Query> subQueries = Lists.mutable.empty();
					for(int j=0, m=queries.size(); j<m; j++)
					{
						FieldWithBoost field = fields.get(j);
						Query subClause = ((BooleanQuery) queries.get(j)).clauses().get(i).getQuery();
						subQueries.add(field.maybeBoost(subClause));
					}

					result.add(
						new DisjunctionMaxQuery(
							subQueries,
							0.0f
						),
						operator
					);
				}

				return result.build();
			}
			else
			{
				// Should be a TermQuery, boost and merge them directly
				return new DisjunctionMaxQuery(
					queries.collectWithIndex((q, i) -> fields.get(i).maybeBoost(q)),
					0.0f
				);
			}
		}
	}

	/**
	 * Get a phrase query for a sub-query.
	 *
	 * @param builder
	 * @param query
	 * @return
	 */
	private Query getPhraseQuery(
		LocaleSupport locale,
		ListIterable<FieldWithBoost> fields,
		QueryBuilder builder,
		String query
	)
	{
		if(fields.size() == 1)
		{
			FieldWithBoost field = fields.getAny();
			String fieldName = fieldName(locale, field.def);
			Query subQuery = builder.createPhraseQuery(fieldName, query);
			return field.maybeBoost(subQuery);
		}
		else
		{
			return new DisjunctionMaxQuery(
				fields.collect(field -> {
					String fieldName = fieldName(locale, field.def);
					Query subQuery = builder.createPhraseQuery(fieldName, query);
					return field.maybeBoost(subQuery);
				}).toList(),
				0.0f
			);
		}
	}

	private String fieldName(LocaleSupport locale, SearchFieldDef<?> def)
	{
		return typeAhead
			? encounter.index().name(def, locale) + ":type-ahead"
			: encounter.index().name(def, locale);
	}

	private static class FieldWithBoost
	{
		private final SearchFieldDef<?> def;
		private final float boost;

		public FieldWithBoost(SearchFieldDef<?> field, float boost)
		{
			this.def = field;
			this.boost = boost;
		}

		public Query maybeBoost(Query q)
		{
			return boost == 1.0 ? q : new BoostQuery(q, boost);
		}
	}
}
