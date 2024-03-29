package se.l4.silo.engine.index.search.internal;

import java.io.IOException;
import java.io.InputStream;

import org.apache.lucene.document.BinaryDocValuesField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.BytesRef;

import reactor.core.publisher.Flux;
import se.l4.exobytes.streaming.StreamingFormat;
import se.l4.exobytes.streaming.StreamingInput;
import se.l4.exobytes.streaming.Token;
import se.l4.silo.StorageException;
import se.l4.silo.engine.index.IndexDataUpdater;
import se.l4.silo.engine.index.search.SearchField;
import se.l4.silo.engine.index.search.SearchFieldDef;
import se.l4.silo.engine.index.search.locales.LocaleSupport;
import se.l4.silo.engine.index.search.locales.Locales;
import se.l4.silo.engine.index.search.types.SearchFieldType;
import se.l4.silo.index.search.SearchIndexException;

public class SearchIndexDataUpdater<T>
	implements IndexDataUpdater
{
	private final Locales locales;

	private final SearchIndexEncounterImpl<T> encounter;
	private final IndexWriter writer;
	private final IndexSearcherManager searchManager;

	private final CommitManager commitManager;

	public SearchIndexDataUpdater(
		Locales locales,

		SearchIndexEncounterImpl<T> encounter,

		IndexWriter writer,
		IndexSearcherManager searcherManager,
		CommitManager commitManager
	)
	{
		this.locales = locales;

		this.encounter = encounter;
		this.writer = writer;
		this.searchManager = searcherManager;
		this.commitManager = commitManager;
	}

	@Override
	public long getLastHardCommit()
	{
		return commitManager.getHardCommit();
	}

	@Override
	public Flux<Long> hardCommits()
	{
		return commitManager.getHardCommits();
	}

	@Override
	public void clear()
	{
		try
		{
			writer.deleteAll();
			commitManager.reinitialize();
			writer.commit();
		}
		catch(IOException e)
		{
			throw new SearchIndexException("Unable to clear index");
		}
	}

	@Override
	public void apply(long op, long id, InputStream rawIn)
		throws IOException
	{
		int version = rawIn.read();
		if(version != 0)
		{
			throw new StorageException("Unknown search index version encountered: " + version);
		}

		searchManager.willMutate(false);

		Document doc = new Document();

		BytesRef idRef = new BytesRef(serializeId(id));

		FieldType ft = new FieldType();
		ft.setStored(true);
		ft.setTokenized(false);
		ft.setIndexOptions(IndexOptions.DOCS);
		doc.add(new Field("_:id", idRef, ft));
		doc.add(new BinaryDocValuesField("_:id", idRef));

		LocaleSupport defaultLangSupport = locales.get("en").get();

		try(StreamingInput in = StreamingFormat.CBOR.createInput(rawIn))
		{
			in.next(Token.VALUE);
			String rawLocale = in.readString();
			doc.add(new Field("_:lang", rawLocale, StringField.TYPE_STORED));

			// Resolve locale support
			LocaleSupport specificLanguageSupport = locales.getOrDefault(rawLocale);

			in.next(Token.LIST_START);

			while(in.peek() != Token.LIST_END)
			{
				in.next(Token.LIST_START);

				in.next(Token.VALUE);
				String fieldName = in.readString();

				SearchField<T, ?> field = encounter.getField(fieldName);
				if(field == null)
				{
					in.skipNext();
					continue;
				}

				if(in.peek() == Token.NULL)
				{
					in.next();
					addField(doc, defaultLangSupport, specificLanguageSupport, field, null);
				}
				else if(in.peek() == Token.LIST_START)
				{
					// Stored a list of values, extract and index them
					in.next(Token.LIST_START);

					while(in.peek() != Token.LIST_END)
					{
						Object value = field.getDefinition().getType().read(in);
						addField(doc, defaultLangSupport, specificLanguageSupport, field, value);
					}

					in.next(Token.LIST_END);
				}
				else
				{
					Object value = field.getDefinition().getType().read(in);
					addField(doc, defaultLangSupport, specificLanguageSupport, field, value);
				}

				in.next(Token.LIST_END);
			}

			in.next(Token.LIST_END);
		}

		// Update the index
		Term idTerm = new Term("_:id", idRef);
		try
		{
			writer.updateDocument(idTerm, doc);
		}
		catch(IOException e)
		{
			throw new StorageException("Unable to update search index; " + e.getMessage(), e);
		}

		// Tell our commit policy that we have modified the index
		commitManager.indexModified(op);
	}

	private <V> void addField(
		Document document,
		LocaleSupport fallback,
		LocaleSupport current,
		SearchField<T, ?> field,
		V object
	)
	{
		if(object == null)
		{
			// Store null = true
			String fieldName = encounter.nullName(field.getDefinition());
			document.add(new Field(fieldName, NullFields.VALUE_NULL, NullFields.FIELD_TYPE));
			return;
		}

		// Store null = false
		String fieldName = encounter.nullName(field.getDefinition());
		document.add(new Field(fieldName, NullFields.VALUE_NON_NULL, NullFields.FIELD_TYPE));

		// Index the actual field
		SearchFieldDef<?> def = field.getDefinition();
		SearchFieldType type = ((SearchFieldType) def.getType());
		if(type.isLocaleSupported() && def.isLanguageSpecific() && fallback != current)
		{
			type.create(new FieldCreationEncounterImpl<V>(
				encounter,
				document::add,
				(SearchField) field,
				current,
				object
			));
		}

		type.create(new FieldCreationEncounterImpl<V>(
			encounter,
			document::add,
			(SearchField) field,
			fallback,
			object
		));
	}

	@Override
	public void delete(long op, long id)
	{
		BytesRef idRef = new BytesRef(serializeId((id)));
		try
		{
			searchManager.willMutate(true);

			writer.deleteDocuments(new Term("_:id", idRef));
		}
		catch(IOException e)
		{
			throw new StorageException("Unable to delete from search index; " + e.getMessage(), e);
		}

		// Tell our commit policy that we have modified the index
		commitManager.indexModified(op);
	}

	private static final byte[] serializeId(long id)
	{
		return new byte[] {
			(byte) id,
			(byte) (id >> 8),
			(byte) (id >> 16),
			(byte) (id >> 24),
			(byte) (id >> 32),
			(byte) (id >> 40),
			(byte) (id >> 48),
			(byte) (id >> 56)
		};
	}
}
