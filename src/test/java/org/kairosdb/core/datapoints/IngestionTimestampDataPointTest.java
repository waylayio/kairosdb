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

package org.kairosdb.core.datapoints;

import org.junit.Test;
import org.kairosdb.core.DataPoint;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IngestionTimestampDataPointTest
{
	@Test
	public void testWrapsDataPointWithIngestionTimestamp()
	{
		// Given
		DataPoint mockDataPoint = mock(DataPoint.class);
		when(mockDataPoint.getTimestamp()).thenReturn(1000L);
		when(mockDataPoint.isLong()).thenReturn(true);
		when(mockDataPoint.getLongValue()).thenReturn(42L);
		
		long ingestionTimestamp = 2000L;
		
		// When
		IngestionTimestampDataPoint wrappedDataPoint = new IngestionTimestampDataPoint(mockDataPoint, ingestionTimestamp);
		
		// Then
		assertThat(wrappedDataPoint.getIngestionTimestamp(), equalTo(ingestionTimestamp));
		assertThat(wrappedDataPoint.getTimestamp(), equalTo(1000L));
		assertThat(wrappedDataPoint.isLong(), equalTo(true));
		assertThat(wrappedDataPoint.getLongValue(), equalTo(42L));
	}
}