package se.l4.silo.engine.builder;

import org.apache.bval.jsr303.xml.FieldType;

public interface FieldLimitBuilder<R>
{
	FieldLimitBuilder<R> setType(FieldType type);
	
	FieldLimitBuilder<R> setLength(int length);
	
	FieldLimitBuilder<R> collection();
	
	R done();
}
