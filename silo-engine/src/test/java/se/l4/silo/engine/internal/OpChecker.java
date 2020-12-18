package se.l4.silo.engine.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * Helper to check against actual operations against a set of expected
 * operations.
 *
 * @author Andreas Holstenson
 *
 */
public class OpChecker
{
	private final LinkedList<Object[]> queue;

	public OpChecker()
	{
		queue = new LinkedList<>();
	}

	public void expect(Object... data)
	{
		queue.add(data);
	}

	public void check(Object... data)
	{
		if(queue.isEmpty())
		{
			throw new AssertionError("No more expected operations");
		}

		Object[] first = queue.poll();
		if(first.length != data.length)
		{
			throw new AssertionError("Operation did not match, expected: "
				+ Arrays.toString(first) + " but got " + Arrays.toString(data));
		}

		boolean matches = true;
		for(int i=0, n=first.length; i<n; i++)
		{
			Object a = first[i];
			Object b = data[i];

			if(a == b) continue;
			if(a == null || b == null)
			{
				matches = false;
			}
			else if(a instanceof InputStream)
			{
				try(InputStream in1 = (InputStream) a; InputStream in2 = (InputStream) b)
				{
					int r1;
					int idx = 0;
					while((r1 = in1.read()) != -1)
					{
						int r2 = in2.read();
						if(r1 != r2)
						{
							throw new AssertionError("Operation did not match for argument "
									+ i + ", of type bytes, mismatch on index " + idx);
						}

						idx++;
					}

				}
				catch(IOException e)
				{
					throw new RuntimeException(e);
				}
			}
			else if(! a.equals(b))
			{
				matches = false;
			}

			if(! matches)
			{
				throw new AssertionError("Operation did not match for argument "
					+ i + ", expected: "
					+ Arrays.toString(first) + " but got " + Arrays.toString(data));
			}
		}

	}

	public void checkEmpty()
	{
		if(! queue.isEmpty())
		{
			throw new AssertionError("Found trailing operations, not everything has been applied");
		}
	}
}
