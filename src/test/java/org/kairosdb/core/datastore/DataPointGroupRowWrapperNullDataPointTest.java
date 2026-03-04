package org.kairosdb.core.datastore;

import org.junit.Test;
import org.kairosdb.core.DataPoint;
import org.kairosdb.core.datapoints.LongDataPoint;

import java.util.Collections;
import java.util.Set;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;


public class DataPointGroupRowWrapperNullDataPointTest
{
	@Test
	public void next_throws_when_wrapped_row_returns_null_data_point()
	{
		DataPointRow brokenRow = new DataPointRow()
		{
			private int index = 0;

			@Override
			public String getName()
			{
				return "metric.name";
			}

			@Override
			public String getDatastoreType()
			{
				return "long";
			}

			@Override
			public Set<String> getTagNames()
			{
				return Collections.singleton("host");
			}

			@Override
			public String getTagValue(String tag)
			{
				return "srv-1";
			}

			@Override
			public void close()
			{
			}

			@Override
			public int getDataPointCount()
			{
				return 2;
			}

			@Override
			public boolean hasNext()
			{
				return index < 2;
			}

			@Override
			public DataPoint next()
			{
				index++;
				if (index == 1)
				{
					return new LongDataPoint(1000L, 42L);
				}
				return null;
			}

			@Override
			public void remove()
			{
			}
		};

		DataPointGroupRowWrapper wrappedRow = new DataPointGroupRowWrapper(brokenRow);
		SortingDataPointGroup sorted = new SortingDataPointGroup("metric.name", Order.ASC);
		sorted.addIterator(wrappedRow);

		try
		{
			sorted.next();
			fail("Expected IllegalStateException when DataPointRow returns null");
		}
		catch (IllegalStateException ise)
		{
			assertThat(ise.getMessage(), containsString("DataPointRow returned null DataPoint"));
			assertThat(ise.getMessage(), containsString("metric.name"));
		}
	}
}
