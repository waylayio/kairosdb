/*
 * Copyright 2016 KairosDB Authors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.kairosdb.core.aggregator;

import com.google.inject.Inject;
import org.kairosdb.core.DataPoint;
import org.kairosdb.core.annotation.FeatureComponent;
import org.kairosdb.core.datapoints.DoubleDataPointFactory;

import java.util.Collections;
import java.util.Iterator;

/**
 Converts all longs to double. This will cause a loss of precision for very large long values.
 */
@FeatureComponent(
        name = "first",
		description = "Returns the first value data point for the time range."
)
public class FirstAggregator extends RangeAggregator
{

	@Inject
	public FirstAggregator()
	{
	}

	@Override
	public boolean canAggregate(String groupType)
	{
		return true;
	}

	@Override
	public String getAggregatedGroupType(String groupType)
	{
		return groupType;
	}

	@Override
	protected RangeSubAggregator getSubAggregator()
	{
		return (new FirstDataPointAggregator());
	}

	private class FirstDataPointAggregator implements RangeSubAggregator
	{
		@Override
		public Iterable<DataPoint> getNextDataPoints(long returnTime, Iterator<DataPoint> dataPointRange)
		{
			DataPoint first = null;
			long firstTime = Long.MIN_VALUE;
			while (dataPointRange.hasNext())
			{
				DataPoint current = dataPointRange.next();
				if(first == null){
					firstTime = current.getTimestamp();
					first = current;
					first.setTimestamp(returnTime);
				}else {
					if (current.getTimestamp() < firstTime) {
						firstTime = current.getTimestamp();
						first = current;
						first.setTimestamp(returnTime);

					}
				}
			}

			return first != null ? Collections.singletonList(first) : Collections.emptyList();
		}
	}
}
