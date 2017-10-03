package se.l4.silo.engine.search.query;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CachingTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import com.google.common.collect.ImmutableList;

import se.l4.silo.engine.search.FieldDefinition;
import se.l4.silo.engine.search.IndexDefinition;
import se.l4.silo.engine.search.Language;
import se.l4.silo.engine.search.SearchFieldType;
import se.l4.silo.search.query.UserQueryData;

public class UserQueryParser
{
	private static final Term[] EMPTY_TERM = new Term[0];

	private final IndexDefinition def;
	private final List<FieldWithBoost> fields;
	private final boolean usePrefix;

	public UserQueryParser(IndexDefinition def, boolean usePrefix)
	{
		this.def = def;
		this.usePrefix = usePrefix;

		fields = new ArrayList<>();
	}

	public UserQueryParser addField(String name, float boost)
	{
		fields.add(new FieldWithBoost(def.getField(name), boost));

		return this;
	}

	public UserQueryParser addField(FieldDefinition def, float boost)
	{
		fields.add(new FieldWithBoost(def, boost));

		return this;
	}

	public Query parse(QueryParseEncounter<UserQueryData> encounter)
		throws IOException
	{
		UserQueryData data = encounter.data();
		for(String f : data.getFields())
		{
			addField(f, 1f);
		}

		String query = data.getQuery().trim();

		if(! encounter.isSpecificLanguage())
		{
			return parseQuery(query, encounter.currentLanguage());
		}
		else
		{
			DisjunctionMaxQuery result = new DisjunctionMaxQuery(ImmutableList.of(
				parseQuery(query, encounter.currentLanguage()),
				parseQuery(query, encounter.defaultLanguage())
			), 0.0f);
			return result;
		}
	}

	public Query parseDirect(QueryParseEncounter<String> encounter)
		throws IOException
	{
		String query = encounter.data().trim();

		if(! encounter.isSpecificLanguage())
		{
			return parseQuery(query, encounter.currentLanguage());
		}
		else
		{
			DisjunctionMaxQuery result = new DisjunctionMaxQuery(ImmutableList.of(
				parseQuery(query, encounter.currentLanguage()),
				parseQuery(query, encounter.defaultLanguage())
			), 0.0f);
			return result;
		}
	}

	private Query parseQuery(String query, Language lang)
		throws IOException
	{
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
					Query fq = getFieldQuery(query.substring(last, i), lang, true);
					if(fq != null)
					{
						result.add(fq, BooleanClause.Occur.MUST);
					}
				}
				else if(last < i-1)
				{
					Query fq = getFieldQuery(query.substring(last, i), lang, true);
					if(fq != null)
					{
						result.add(fq, BooleanClause.Occur.MUST);
					}
				}

				last = i+1;
				inQuote = ! inQuote;
			}
			else if(Character.isWhitespace(c) && ! inQuote)
			{
				if(last < i-1)
				{
					Query fq = getFieldQuery(query.substring(last, i), lang, false);
					if(fq != null)
					{
						result.add(fq, BooleanClause.Occur.MUST);
					}
				}

				last = i+1;
			}
		}

		if(inQuote)
		{
			Query fq = getFieldQuery(query.substring(last, query.length()), lang, true);
			if(fq != null)
			{
				result.add(fq, BooleanClause.Occur.MUST);
			}
		}
		else if(last < query.length())
		{
			Query fq = getFieldQuery(query.substring(last, query.length()), lang, true);
			if(fq != null)
			{
				result.add(fq, BooleanClause.Occur.MUST);
			}
		}

		return result.build();
	}

	protected DisjunctionMaxQuery getFieldQuery(String queryText, Language lang, boolean quoted)
		throws IOException
	{
		List<Query> queries = new ArrayList<>();
		boolean hasResults = false;
		for(FieldWithBoost fb : fields)
		{
			Query q = getFieldQuery(queryText, lang, quoted, fb.field, false);
			if(q != null)
			{
				q = new BoostQuery(q, fb.boost);
				queries.add(q);
				hasResults = true;
			}
//
//			q = getFieldQuery(queryText, language, false, fb.field, true);
//			if(q != null)
//			{
//				q.setBoost(0.6f);
//				result.add(q);
//			}
		}

		return hasResults ? new DisjunctionMaxQuery(queries, 0.0f) : null;
	}

	protected Query getFieldQuery(String queryText, Language lang, boolean quoted, FieldDefinition fdef, boolean fuzzy)
		throws IOException
	{
		// Use the analyzer to get all the tokens, and then build a TermQuery,
		// PhraseQuery, or nothing based on the term count

		String field = fdef.name(fdef.getName(), lang);
		SearchFieldType ft = fdef.getType();
		Analyzer analyzer = ft.getAnalyzer(lang);

		try(TokenStream source = analyzer.tokenStream(field, new StringReader(queryText));
			CachingTokenFilter buffer = new CachingTokenFilter(source))
		{
			CharTermAttribute termAtt = null;
			PositionIncrementAttribute posIncrAtt = null;
			int numTokens = 0;

			boolean success = false;
			buffer.reset();
			success = true;

			if(success)
			{
				if(buffer.hasAttribute(CharTermAttribute.class))
				{
					termAtt = buffer.getAttribute(CharTermAttribute.class);
				}
				if(buffer.hasAttribute(PositionIncrementAttribute.class))
				{
					posIncrAtt = buffer
							.getAttribute(PositionIncrementAttribute.class);
				}
			}

			int positionCount = 0;
			boolean severalTokensAtSamePosition = false;

			boolean hasMoreTokens = false;
			if(termAtt != null)
			{
				try
				{
					hasMoreTokens = buffer.incrementToken();
					while(hasMoreTokens)
					{
						numTokens++;
						int positionIncrement = (posIncrAtt != null) ?
							posIncrAtt.getPositionIncrement() : 1;

						if(positionIncrement != 0)
						{
							positionCount += positionIncrement;
						}
						else
						{
							severalTokensAtSamePosition = true;
						}
						hasMoreTokens = buffer.incrementToken();
					}
				}
				catch(IOException e)
				{
					// ignore
				}
			}
			try
			{
				// rewind the buffer stream
				buffer.reset();

				// close original stream - all tokens buffered
				source.close();
			}
			catch(IOException e)
			{
				// ignore
			}

			if(numTokens == 0)
			{
				return null;
			}
			else if(numTokens == 1)
			{
				String term = null;
				try
				{
					boolean hasNext = buffer.incrementToken();
					assert hasNext == true;
					term = termAtt.toString();
				}
				catch(IOException e)
				{
					// safe to ignore, because we know the number of tokens
				}
				return usePrefix
					? new PrefixQuery(new Term(field, term))
					: (fuzzy
						? new FuzzyQuery(new Term(field, term), 1)
						: new TermQuery(new Term(field, term)));
			}
			else
			{
				if(severalTokensAtSamePosition || !quoted)
				{
					if(positionCount == 1 || !quoted)
					{
						// no phrase query:
						BooleanQuery.Builder builder = new BooleanQuery.Builder();
						builder.setDisableCoord(positionCount == 1);

						BooleanClause.Occur occur = BooleanClause.Occur.SHOULD;

						for(int i = 0; i < numTokens; i++)
						{
							String term = null;
							try
							{
								boolean hasNext = buffer.incrementToken();
								assert hasNext == true;
								term = termAtt.toString();
							}
							catch(IOException e)
							{
								// safe to ignore, because we know the number of
								// tokens
							}

							Query currentQuery = usePrefix
								? new PrefixQuery(new Term(field, term))
								: (fuzzy
									? new FuzzyQuery(new Term(field, term), 1)
									: new TermQuery(new Term(field, term)));
							builder.add(currentQuery, occur);
						}
						return builder.build();
					}
					else
					{
						// phrase query:
						MultiPhraseQuery.Builder builder = new MultiPhraseQuery.Builder();
	//					mpq.setSlop(phraseSlop);
						List<Term> multiTerms = new ArrayList<>();
						int position = -1;
						for(int i = 0; i < numTokens; i++)
						{
							String term = null;
							int positionIncrement = 1;
							try
							{
								boolean hasNext = buffer.incrementToken();
								assert hasNext == true;
								term = termAtt.toString();
								if(posIncrAtt != null)
								{
									positionIncrement = posIncrAtt
											.getPositionIncrement();
								}
							}
							catch(IOException e)
							{
								// safe to ignore, because we know the number of
								// tokens
							}

							if(positionIncrement > 0 && multiTerms.size() > 0)
							{
								builder.add(multiTerms.toArray(EMPTY_TERM), position);
								multiTerms.clear();
							}
							position += positionIncrement;
							multiTerms.add(new Term(field, term));
						}

						builder.add(multiTerms.toArray(EMPTY_TERM), position);

						return builder.build();
					}
				}
				else
				{
					PhraseQuery.Builder pq = new PhraseQuery.Builder();
	//				pq.setSlop(phraseSlop);
					int position = -1;

					for(int i = 0; i < numTokens; i++)
					{
						String term = null;
						int positionIncrement = 1;

						try
						{
							boolean hasNext = buffer.incrementToken();
							assert hasNext == true;
							term = termAtt.toString();
							if(posIncrAtt != null)
							{
								positionIncrement = posIncrAtt.getPositionIncrement();
							}
						}
						catch(IOException e)
						{
							// safe to ignore, because we know the number of tokens
						}

						position += positionIncrement;
						pq.add(new Term(field, term), position);
					}

					return pq.build();
				}
			}
		}
	}

	private static class FieldWithBoost
	{
		private final FieldDefinition field;
		private final float boost;

		public FieldWithBoost(FieldDefinition field, float boost)
		{
			this.field = field;
			this.boost = boost;
		}
	}
}
