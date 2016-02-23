package se.l4.silo.engine.internal.query;

import se.l4.silo.engine.DataEncounter;
import se.l4.silo.engine.QueryEncounter;
import se.l4.silo.engine.QueryEngine;

public class IndexQueryEngine<T>
	implements QueryEngine<T>
{

	@Override
	public void query(QueryEncounter<T> encounter)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void update(long id, DataEncounter encounter)
	{
		System.out.println("Update " + id);
	}

	@Override
	public void delete(long id)
	{
		System.out.println("Delete " + id);
	}

}
