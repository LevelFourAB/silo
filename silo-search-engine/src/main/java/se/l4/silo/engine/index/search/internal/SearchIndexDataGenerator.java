package se.l4.silo.engine.index.search.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Locale;
import java.util.function.Function;

import org.eclipse.collections.api.RichIterable;

import se.l4.exobytes.streaming.StreamingFormat;
import se.l4.exobytes.streaming.StreamingOutput;
import se.l4.silo.engine.index.IndexDataGenerator;
import se.l4.silo.engine.index.search.SearchField;
import se.l4.silo.engine.index.search.SearchFieldDef;
import se.l4.silo.engine.index.search.types.SearchFieldType;
import se.l4.silo.index.search.SearchIndexException;

public class SearchIndexDataGenerator<T>
	implements IndexDataGenerator<T>
{
	private final Function<T, Locale> localeSupplier;
	private final RichIterable<SearchField<T, ?>> fields;

	public SearchIndexDataGenerator(
		Function<T, Locale> localeSupplier,
		RichIterable<SearchField<T, ?>> fields
	)
	{
		this.localeSupplier = localeSupplier;
		this.fields = fields;
	}

	@Override
	public void generate(T data, OutputStream rawOut)
		throws IOException
	{
		// Write a version tag
		rawOut.write(0);

		// Number of fields that are going to be written
		try(StreamingOutput out = StreamingFormat.CBOR.createOutput(rawOut))
		{
			// Write the locale of the item first
			Locale locale = localeSupplier.apply(data);
			if(locale == null)
			{
				locale = Locale.ENGLISH;
			}
			out.writeString(locale.toLanguageTag());

			// Write all of the extracted fields
			out.writeListStart(fields.size());

			for(SearchField<T, ?> field : fields)
			{
				out.writeListStart(2);

				SearchFieldDef<T> def = field.getDefinition();

				// Write the name of the field
				out.writeString(def.getName());

				// Get and write the value
				SearchFieldType type = def.getType();
				if(def instanceof SearchFieldDef.Single)
				{
					Object value = ((SearchFieldDef.Single) def).getSupplier().apply(data);
					if(value == null)
					{
						out.writeNull();
					}
					else
					{
						type.write(value, out);
					}
				}
				else if(def instanceof SearchFieldDef.Collection)
				{
					Object value = ((SearchFieldDef.Collection) def).getSupplier().apply(data);
					if(value == null)
					{
						out.writeNull();
					}
					else if(value instanceof RichIterable)
					{
						RichIterable<?> iterable = (RichIterable<?>) value;
						out.writeListStart(iterable.size());

						for(Object o : iterable)
						{
							type.write(o, out);
						}

						out.writeListEnd();
					}
					else if(value instanceof Collection)
					{
						Collection<?> iterable = (Collection<?>) value;
						out.writeListStart(iterable.size());

						for(Object o : iterable)
						{
							type.write(o, out);
						}

						out.writeListEnd();
					}
					else if(value instanceof Iterable)
					{
						out.writeListStart();

						for(Object o : (Iterable<?>) value)
						{
							type.write(o, out);
						}

						out.writeListEnd();
					}
					else
					{
						throw new SearchIndexException("Expected instance of Iterable but received " + value);
					}
				}
				else
				{
					throw new SearchIndexException("Unknown type of field definition");
				}

				out.writeListEnd();
			}

			out.writeListEnd();
		}
	}
}
