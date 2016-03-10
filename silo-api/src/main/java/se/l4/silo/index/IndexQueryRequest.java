package se.l4.silo.index;

import java.util.ArrayList;
import java.util.List;

import se.l4.aurochs.serialization.AllowAny;
import se.l4.aurochs.serialization.Expose;
import se.l4.aurochs.serialization.ReflectionSerializer;
import se.l4.aurochs.serialization.Use;

@Use(ReflectionSerializer.class)
public class IndexQueryRequest
{
	@Expose
	private List<Criterion> criterias;
	
	public IndexQueryRequest()
	{
	}
	
	public List<Criterion> getCriterias()
	{
		return criterias;
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
		MORA_THAN_OR_EQUAL_TO
	}
}
