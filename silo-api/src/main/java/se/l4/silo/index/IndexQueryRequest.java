package se.l4.silo.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import se.l4.commons.serialization.AllowAny;
import se.l4.commons.serialization.Expose;
import se.l4.commons.serialization.ReflectionSerializer;
import se.l4.commons.serialization.Use;

@Use(ReflectionSerializer.class)
public class IndexQueryRequest
{
	@Expose
	private int offset;
	@Expose
	private int limit;
	
	@Expose
	private List<Criterion> criterias;
	@Expose
	private List<SortOnField> sort;
	@Expose
	private boolean reverseDefaultSort;
	
	public IndexQueryRequest()
	{
	}
	
	public int getOffset()
	{
		return offset;
	}
	
	public void setOffset(int offset)
	{
		this.offset = offset;
	}
	
	public int getLimit()
	{
		return limit;
	}
	
	public void setLimit(int limit)
	{
		this.limit = limit;
	}
	
	public List<Criterion> getCriterias()
	{
		return criterias == null ? Collections.emptyList() : criterias;
	}
	
	public void addCritera(String field, Op op, Object value)
	{
		if(criterias == null)
		{
			criterias = new ArrayList<>();
		}
		
		Criterion c = new Criterion();
		c.field = field;
		c.op = op;
		c.value = value;
		
		criterias.add(c);
	}
	
	public List<SortOnField> getSort()
	{
		return sort == null ? Collections.emptyList() : sort;
	}
	
	public void addSort(String field, boolean ascending)
	{
		if(sort == null)
		{
			sort = new ArrayList<>();
		}
			
		SortOnField s = new SortOnField();
		s.field = field;
		s.ascending = ascending;
		
		sort.add(s);
	}
	
	public boolean isReverseDefaultSort()
	{
		return reverseDefaultSort;
	}
	
	public void setReverseDefaultSort(boolean reverseDefaultSort)
	{
		this.reverseDefaultSort = reverseDefaultSort;
	}
	
	@Use(ReflectionSerializer.class)
	public static class Criterion
	{
		@Expose
		private String field;
		@Expose
		private Op op;
		@Expose
		@AllowAny
		private Object value;
		
		public String getField()
		{
			return field;
		}
		
		public Op getOp()
		{
			return op;
		}
		
		public Object getValue()
		{
			return value;
		}
	}
	
	public enum Op
	{
		EQUAL,
		NOT_EQUAL,
		LESS_THAN,
		LESS_THAN_OR_EQUAL_TO,
		MORE_THAN,
		MORE_THAN_OR_EQUAL_TO
	}
	
	@Use(ReflectionSerializer.class)
	public static class SortOnField
	{
		@Expose
		private String field;
		@Expose
		private boolean ascending;
		
		public String getField()
		{
			return field;
		}
		
		public boolean isAscending()
		{
			return ascending;
		}
	}
}
