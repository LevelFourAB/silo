package se.l4.silo.engine.internal;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
	BinaryEntityTest.class,
	StructuredEntityTest.class,
	ObjectEntityTest.class
})
public class EntitySuite
{

}
