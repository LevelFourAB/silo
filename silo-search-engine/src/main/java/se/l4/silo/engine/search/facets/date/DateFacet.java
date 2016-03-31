package se.l4.silo.engine.search.facets.date;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Function;

import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.facet.range.LongRange;
import org.apache.lucene.facet.range.LongRangeFacetCounts;

import com.google.common.collect.Lists;

import se.l4.silo.engine.search.FieldDefinition;
import se.l4.silo.engine.search.IndexDefinitionEncounter;
import se.l4.silo.engine.search.SearchFields;
import se.l4.silo.engine.search.facets.Facet;
import se.l4.silo.engine.search.facets.FacetCollectionEncounter;
import se.l4.silo.search.DefaultFacetEntry;
import se.l4.silo.search.FacetEntry;
import se.l4.silo.search.facet.SimpleFacetQuery;

/**
 * {@link Facet} for counting the number of documents in different date ranges.
 * Supports {@link SearchFields#LONG} for storing the time as milliseconds.
 * 
 * The following ranges are returned: today, yesterday, lastWeek, lastMonth
 * and other.
 * 
 * @author Andreas Holstenson
 *
 */
public class DateFacet
	implements Facet<SimpleFacetQuery>
{
	private final String field;

	public DateFacet(String field)
	{
		this.field = field;
	}

	@Override
	public String type()
	{
		return "date";
	}
	
	@Override
	public void setup(IndexDefinitionEncounter encounter)
	{
		encounter.addValuesField(field);
	}
	
	private LongRange between(String id, ZonedDateTime first, ZonedDateTime last)
	{
		return new LongRange(id, first.toEpochSecond() * 1000, true, last.toEpochSecond() * 1000, false);
	}

	@Override
	public List<FacetEntry> collect(FacetCollectionEncounter<SimpleFacetQuery> encounter)
		throws IOException
	{
		FieldDefinition fieldDef = encounter.getIndexDefinition()
			.getField(this.field);
		
		String field = fieldDef.docValuesName(encounter.getLocale());
		
		int count = encounter.getQueryParameters().getCount();
		
		ZonedDateTime dt = ZonedDateTime.now(); // TODO: Support timezone
		ZonedDateTime midnight = dt.truncatedTo(ChronoUnit.DAYS);
		ZonedDateTime midnightYesterday = midnight.minusDays(1);
		ZonedDateTime lastWeek = midnightYesterday.minusWeeks(1);
		ZonedDateTime lastMonth = lastWeek.minusMonths(1);
		
		Facets f = new LongRangeFacetCounts(
			field,
			encounter.getCollector(),
			between("today", midnight, dt),
			between("yesterday", midnightYesterday, midnight),
			between("lastWeek", lastWeek, midnightYesterday),
			between("lastMonth", lastMonth, lastWeek),
			new LongRange("other", 0, true, System.currentTimeMillis(), true)
		);
		
		List<FacetEntry> entries = Lists.newArrayList();
		for(LabelAndValue lv : f.getTopChildren(count, field).labelValues)
		{
			entries.add(new DefaultFacetEntry(lv.label, lv.value.intValue(), null));
		}
		return entries;
	}

	public static <Parent> DateFacetBuilder<Parent> newFacet(Function<Facet<?>, Parent> configReceiver)
	{
		return new DateFacetBuilder<>(configReceiver);
	}
}
