package se.l4.silo.engine.internal;

import java.util.Optional;

import com.google.common.collect.ImmutableMap;

import se.l4.silo.engine.FieldDef;
import se.l4.silo.engine.Fields;

/**
 * Implementation of {@link Fields}.
 *
 * @author Andreas Holstenson
 *
 */
public class FieldsImpl
	implements Fields
{
	private final ImmutableMap<String, FieldDef> fields;

	public FieldsImpl(Iterable<FieldDef> defs)
	{
		ImmutableMap.Builder<String, FieldDef> builder = ImmutableMap.builder();
		for(FieldDef def : defs)
		{
			builder.put(def.getName(), def);
		}

		this.fields = builder.build();
	}

	@Override
	public Optional<FieldDef> get(String name)
	{
		return Optional.ofNullable(fields.get(name));
	}

}
