package se.l4.silo.engine.search.facets.category;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Function;

import org.apache.lucene.facet.FacetsCollector.MatchingDocs;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.BytesRef;

import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import com.carrotsearch.hppc.cursors.ObjectIntCursor;
import com.google.common.collect.Lists;

import se.l4.silo.engine.search.FieldDefinition;
import se.l4.silo.engine.search.IndexDefinitionEncounter;
import se.l4.silo.engine.search.NumericSearchField;
import se.l4.silo.engine.search.SearchFields;
import se.l4.silo.engine.search.facets.Facet;
import se.l4.silo.engine.search.facets.FacetCollectionEncounter;
import se.l4.silo.search.DefaultFacetEntry;
import se.l4.silo.search.FacetEntry;
import se.l4.silo.search.facet.SimpleFacetQuery;

/**
 * {@link Facet} that counts how many results fit in a set of categories.
 * Can be used with {@link SearchFields#TOKEN}, {@link SearchFields#LONG}
 * and {@link SearchFields#INTEGER}.
 * 
 * @author Andreas Holstenson
 *
 */
public class CategoryFacet
	implements Facet<SimpleFacetQuery>
{
	private final String field;

	/**
	 * Create a new instance of this facet for the given field.
	 * 
	 * @param field
	 */
	public CategoryFacet(String field)
	{
		this.field = field;
	}

	@Override
	public String type()
	{
		return "category";
	}
	
	@Override
	public void setup(IndexDefinitionEncounter encounter)
	{
		encounter.addValuesField(field);
	}

	@Override
	public List<FacetEntry> collect(FacetCollectionEncounter<SimpleFacetQuery> encounter)
		throws IOException
	{
		FieldDefinition fieldDef = encounter.getIndexDefinition()
			.getField(this.field);
		
		String field = fieldDef.docValuesName(encounter.getLocale());
		
		SimpleFacetQuery parameters = encounter.getQueryParameters();
		int count = parameters.getCount();
		
		int totalHits = 0;
		ObjectIntMap<String> result = new ObjectIntHashMap<>();
		for(MatchingDocs docs : encounter.getCollector().getMatchingDocs())
		{
			totalHits += docs.totalHits;
			LeafReader reader = docs.context.reader();
			
			if(fieldDef.getType() instanceof NumericSearchField)
			{
				// NumericSearchfield uses NumericDocValues
				NumericDocValues values = reader.getNumericDocValues(field);
				if(values == null) continue;
				
				DocIdSetIterator it = docs.bits.iterator();
				int doc;
				while((doc = it.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS)
				{
					long value = values.get(doc);
					result.addTo(String.valueOf(value), 1);
				}
			}
			else
			{
				// Assume other fields use SortedSetDocValues
				SortedSetDocValues values = reader.getSortedSetDocValues(field);
				if(values == null) continue;
				
				DocIdSetIterator it = docs.bits.iterator();
				int doc;
				while((doc = it.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS)
				{
					values.setDocument(doc);
					long ord;
					while((ord = values.nextOrd()) != SortedSetDocValues.NO_MORE_ORDS)
					{
						BytesRef ref = values.lookupOrd(ord);
						result.addTo(ref.utf8ToString(), 1);
					}
				}
			}
		}
	
		TreeSet<Result> tree = new TreeSet<>();
		for(ObjectIntCursor<String> cursor : result)
		{
			Result r = new Result(cursor.key, cursor.value);
			if(count == 0 || tree.size() < count)
			{
				tree.add(r);
			}
			else if(tree.last().compareTo(r) > 0)
			{
				tree.pollLast();
				tree.add(r);
			}
		}
		
		List<FacetEntry> entries = Lists.newArrayList();
		Iterator<Result> it = tree.iterator();
		while(it.hasNext())
		{
			Result r = it.next();
			entries.add(new DefaultFacetEntry(r.key, r.count, r.key));
		}
		
		return entries;
	}
	
	private static class Result
		implements Comparable<Result>
	{
		private final String key;
		private final int count;
		
		public Result(String key, int count)
		{
			this.key = key;
			this.count = count;
		}
		
		@Override
		public int compareTo(Result o)
		{
			int c = Integer.compare(o.count, count);
			if(c != 0) return c;
			
			return key.compareTo(o.key);
		}
	}
	
	public static <Parent> CategoryFacetBuilder<Parent> newFacet(Function<Facet<?>, Parent> configReceiver)
	{
		return new CategoryFacetBuilder<>(configReceiver);
	}
}
