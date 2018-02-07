package se.l4.silo.engine.internal.index;

import java.util.Comparator;

import se.l4.silo.engine.types.FieldType;
import se.l4.silo.engine.types.LongFieldType;

public class Sort
{
	public static Builder builder()
	{
		return new Builder();
	}
	
	public static class Builder
	{
		private Comparator<Result> comparator;
		private boolean idAdded;
		
		public Builder()
		{
		}
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public Builder key(int field, FieldType ft, boolean ascending)
		{
			Comparator<Result> newComp = null;
			
			if(ascending)
			{
				newComp = (r1, r2) -> ft.compare(r1.keys[field], r2.keys[field]);
			}
			else
			{
				newComp = (r1, r2) -> ft.compare(r2.keys[field], r1.keys[field]);
			}
			
			if(comparator == null)
			{
				comparator = newComp;
			}
			else
			{
				comparator = comparator.thenComparing(newComp);
			}
			
			return this;
		}
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public Builder value(int field, FieldType ft, boolean ascending)
		{
			Comparator<Result> newComp = null;
			
			if(ascending)
			{
				newComp = (r1, r2) -> ft.compare(r1.values[field], r2.values[field]);
			}
			else
			{
				newComp = (r1, r2) -> ft.compare(r2.values[field], r1.values[field]);
			}
			
			if(comparator == null)
			{
				comparator = newComp;
			}
			else
			{
				comparator = comparator.thenComparing(newComp);
			}
			
			return this;
		}
		
		public Builder id(boolean ascending)
		{
			if(idAdded) return this;
			
			idAdded = true;
			Comparator<Result> newComp = (r1, r2) -> LongFieldType.INSTANCE.compare(r1.getId(), r2.getId());
			
			if(! ascending)
			{
				newComp = newComp.reversed();
			}
			
			if(comparator == null)
			{
				comparator = newComp;
			}
			else
			{
				comparator = comparator.thenComparing(newComp);
			}
			
			return this;
		}
		
		public Comparator<Result> build()
		{
			return comparator;
		}
	}
}