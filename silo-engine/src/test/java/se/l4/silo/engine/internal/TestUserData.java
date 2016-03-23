package se.l4.silo.engine.internal;

import se.l4.aurochs.serialization.Expose;
import se.l4.aurochs.serialization.ReflectionSerializer;
import se.l4.aurochs.serialization.Use;

@Use(ReflectionSerializer.class)
public class TestUserData
{
	@Expose
	private String name;
	@Expose int age;
	@Expose
	private boolean active;
	
	public TestUserData()
	{
	}
	
	public TestUserData(String name, int age, boolean active)
	{
		super();
		this.name = name;
		this.age = age;
		this.active = active;
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (active ? 1231 : 1237);
		result = prime * result + age;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		TestUserData other = (TestUserData) obj;
		if(active != other.active)
			return false;
		if(age != other.age)
			return false;
		if(name == null)
		{
			if(other.name != null)
				return false;
		}
		else if(!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "TestUserData [name=" + name + ", age=" + age + ", active=" + active + "]";
	}
}