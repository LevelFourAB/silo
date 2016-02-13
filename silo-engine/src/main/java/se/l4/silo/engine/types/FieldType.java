package se.l4.silo.engine.types;

import java.io.IOException;

import se.l4.aurochs.core.io.ExtendedDataInput;
import se.l4.aurochs.core.io.ExtendedDataOutput;
import se.l4.aurochs.serialization.Serializer;

/**
 * Field type abstraction, used to support custom field types.
 * 
 * @author Andreas Holstenson
 *
 * @param <T>
 */
public interface FieldType<T>
{
	/**
	 * Get the unique id of this field type. This is used to reference a
	 * field type in configurations.
	 * 
	 * @return
	 */
	String uniqueId();
	
	/**
	 * Compare the two fields.
	 * 
	 * @param o1
	 * @param o2
	 * @return
	 */
	int compare(T o1, T o2);
	
	/**
	 * Estimate the amount of memory used by the given instance.
	 * 
	 * @param item
	 * @return
	 */
	int estimateMemory(T instance);
	
	/**
	 * Attempt to convert the given input to the type of this field.
	 * 
	 * @param in
	 * @return
	 */
	T convert(Object in);
	
	/**
	 * Get the serializer to use for higher level representation of this
	 * type.
	 * 
	 * @return
	 */
	Serializer<T> getSerializer();
	
	/**
	 * Write the given instance to the output.
	 * 
	 * @param instance
	 * @param out
	 * @throws IOException
	 */
	void write(T instance, ExtendedDataOutput out)
		throws IOException;
	
	/**
	 * Read an instance from the given input.
	 * 
	 * @param instance
	 * @param in
	 * @return
	 * @throws IOException
	 */
	T read(ExtendedDataInput in)
		throws IOException;
}
