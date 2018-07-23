package se.l4.silo.engine.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import se.l4.silo.engine.Fields;
import se.l4.silo.engine.config.FieldConfig;

public class FieldsHelper<T>
	implements HasFieldDefinitions<T>
{
	private final List<FieldConfig> fields;
	private final T parent;

	public FieldsHelper(T parent)
	{
		this.parent = parent;
		fields = new ArrayList<>();
	}

	@Override
	public FieldDefBuilder<T> defineField(String field)
	{
		return new FieldDefBuilder<T>()
		{
			private String type;
			private boolean collection;

			@Override
			public FieldDefBuilder<T> setType(String type)
			{
				this.type = type;
				return this;
			}

			@Override
			public FieldDefBuilder<T> collection()
			{
				collection = true;
				return this;
			}

			@Override
			public T done()
			{
				Objects.requireNonNull(type, "type of field is required");
				fields.add(new FieldConfig(field, type, collection));
				return parent;
			}

		};
	}

	@Override
	public T defineField(String field, String type)
	{
		return defineField(field).setType(type).done();
	}

	/**
	 * Get an instance of {@link Fields} containing the information about
	 * fields that this helper created.
	 *
	 * @return
	 */
	public List<FieldConfig> build()
	{
		return fields;
	}
}
