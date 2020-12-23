package se.l4.silo.engine.search.internal;

import java.io.IOException;

import org.apache.lucene.search.IndexSearcher;

import se.l4.silo.engine.TransactionValue;

public interface IndexSearcherHandle
	extends TransactionValue.Releasable
{
	/**
	 * Get the searcher.
	 *
	 * @return
	 */
	IndexSearcher getSearcher()
		throws IOException;
}
