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

package org.kairosdb.core.datastore;

import org.junit.Test;
import org.kairosdb.core.DataPoint;
import org.kairosdb.core.KairosDataPointFactory;
import org.kairosdb.core.TestDataPointFactory;
import org.kairosdb.core.datapoints.IngestionTimestampDataPoint;
import org.kairosdb.core.datapoints.LegacyDataPointFactory;
import org.kairosdb.core.datapoints.LegacyDoubleDataPoint;
import org.kairosdb.core.datapoints.LegacyLongDataPoint;
import org.kairosdb.core.datapoints.StringDataPoint;
import org.kairosdb.core.datapoints.StringDataPointFactory;

import java.io.IOException;
import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class CachedSearchResultTest
{
	private static KairosDataPointFactory dataPointFactory = new TestDataPointFactory();
	@Test
	public void test_createCachedSearchResult() throws IOException
	{

		String tempFile = System.getProperty("java.io.tmpdir") + "/baseFile";
		CachedSearchResult csResult =
				CachedSearchResult.createCachedSearchResult("metric1", tempFile, dataPointFactory, true);

		long now = System.currentTimeMillis();

		SortedMap<String, String> tags = new TreeMap<>();
		tags.put("host", "A");
		tags.put("client", "foo");
		QueryCallback.DataPointWriter dataPointWriter = csResult.startDataPointSet(LegacyDataPointFactory.DATASTORE_TYPE, tags);

		dataPointWriter.addDataPoint(new LegacyLongDataPoint(now, 42));
		dataPointWriter.addDataPoint(new LegacyDoubleDataPoint(now+1, 42.1));
		dataPointWriter.addDataPoint(new LegacyLongDataPoint(now+2, 43));
		dataPointWriter.addDataPoint(new LegacyDoubleDataPoint(now+3, 43.1));
		dataPointWriter.close();


		tags = new TreeMap<>();
		tags.put("host", "B");
		tags.put("client", "foo");
		dataPointWriter = csResult.startDataPointSet(LegacyDataPointFactory.DATASTORE_TYPE, tags);

		dataPointWriter.addDataPoint(new LegacyLongDataPoint(now, 1));
		dataPointWriter.addDataPoint(new LegacyDoubleDataPoint(now+1, 1.1));
		dataPointWriter.addDataPoint(new LegacyLongDataPoint(now+2, 2));
		dataPointWriter.addDataPoint(new LegacyDoubleDataPoint(now+3, 2.1));

		dataPointWriter.close();
		tags = new TreeMap<>();
		tags.put("host", "A");
		tags.put("client", "bar");
		dataPointWriter = csResult.startDataPointSet(LegacyDataPointFactory.DATASTORE_TYPE, tags);

		dataPointWriter.addDataPoint(new LegacyLongDataPoint(now, 3));
		dataPointWriter.addDataPoint(new LegacyDoubleDataPoint(now+1, 3.1));
		dataPointWriter.addDataPoint(new LegacyLongDataPoint(now+2, 4));
		dataPointWriter.addDataPoint(new LegacyDoubleDataPoint(now+3, 4.1));

		dataPointWriter.close();

		List<DataPointRow> rows = csResult.getRows();

		assertEquals(3, rows.size());

		assertValues(rows.get(0), 42L, 42.1, 43L, 43.1);

		assertValues(rows.get(1), 1L, 1.1, 2L, 2.1);

		assertValues(rows.get(2), 3L, 3.1, 4L, 4.1);

		//Now close rows so data is saved.
		rows.get(0).close();
		rows.get(1).close();
		rows.get(2).close();

		csResult.close();

		//Re-open cached file and verify the data is the same.
		csResult =
				CachedSearchResult.openCachedSearchResult("metric1", tempFile, 100, dataPointFactory, true);

		rows = csResult.getRows();

		assertEquals(3, rows.size());

		assertValues(rows.get(0), 42L, 42.1, 43L, 43.1);

		assertValues(rows.get(1), 1L, 1.1, 2L, 2.1);

		assertValues(rows.get(2), 3L, 3.1, 4L, 4.1);

		rows.get(0).close();
		rows.get(1).close();
		rows.get(2).close();

		csResult.close();

	}

	@Test
	public void test_AddLongsBeyondBufferSize() throws IOException
	{
		String tempFile = System.getProperty("java.io.tmpdir") + "/baseFile";
		CachedSearchResult csResult = CachedSearchResult.createCachedSearchResult(
				"metric2", tempFile, dataPointFactory, true);

		int numberOfDataPoints = CachedSearchResult.WRITE_BUFFER_SIZE * 2;
		QueryCallback.DataPointWriter dataPointWriter = csResult.startDataPointSet(LegacyDataPointFactory.DATASTORE_TYPE, Collections.<String, String>emptySortedMap());

		long now = System.currentTimeMillis();
		for (int i = 0; i < numberOfDataPoints; i++)
		{
			dataPointWriter.addDataPoint(new LegacyLongDataPoint(now, 42));
		}

		dataPointWriter.close();

		List<DataPointRow> rows = csResult.getRows();
		DataPointRow taggedDataPoints = rows.iterator().next();

		int count = 0;
		while(taggedDataPoints.hasNext())
		{
			DataPoint dataPoint = taggedDataPoints.next();
			assertThat(dataPoint.getLongValue(), equalTo(42L));
			count++;
		}

		assertThat(count, equalTo(numberOfDataPoints));

	}

	@Test
	public void test_AddDoublesBeyondBufferSize() throws IOException
	{
		String tempFile = System.getProperty("java.io.tmpdir") + "/baseFile";
		CachedSearchResult csResult = CachedSearchResult.createCachedSearchResult(
				"metric3", tempFile, dataPointFactory, true);

		int numberOfDataPoints = CachedSearchResult.WRITE_BUFFER_SIZE * 2;
		QueryCallback.DataPointWriter dataPointWriter = csResult.startDataPointSet(LegacyDataPointFactory.DATASTORE_TYPE, Collections.<String, String>emptySortedMap());

		long now = System.currentTimeMillis();
		for (int i = 0; i < numberOfDataPoints; i++)
		{
			dataPointWriter.addDataPoint(new LegacyDoubleDataPoint(now, 42.2));
		}

		dataPointWriter.close();

		List<DataPointRow> rows = csResult.getRows();
		DataPointRow taggedDataPoints = rows.iterator().next();

		int count = 0;
		while(taggedDataPoints.hasNext())
		{
			DataPoint dataPoint = taggedDataPoints.next();
			assertThat(dataPoint.getDoubleValue(), equalTo(42.2));
			count++;
		}

		assertThat(count, equalTo(numberOfDataPoints));

	}

	@Test
	public void test_IngestionTimestampDataPoint_Preservation() throws IOException
	{
		String tempFile = System.getProperty("java.io.tmpdir") + "/baseFile_ingestion";
		CachedSearchResult csResult =
				CachedSearchResult.createCachedSearchResult("metric_ingestion", tempFile, dataPointFactory, true);

		long now = System.currentTimeMillis();
		long ingestionTime = now + 5000; // 5 seconds later

		SortedMap<String, String> tags = new TreeMap<>();
		tags.put("host", "A");
		tags.put("client", "foo");
		QueryCallback.DataPointWriter dataPointWriter = csResult.startDataPointSet(LegacyDataPointFactory.DATASTORE_TYPE, tags);

		// Add regular data points
		dataPointWriter.addDataPoint(new LegacyLongDataPoint(now, 42));

		// Add IngestionTimestampDataPoint
		LegacyLongDataPoint baseDataPoint = new LegacyLongDataPoint(now + 1, 43);
		IngestionTimestampDataPoint ingestionDataPoint = new IngestionTimestampDataPoint(baseDataPoint, ingestionTime);
		dataPointWriter.addDataPoint(ingestionDataPoint);

		// Add another regular data point
		dataPointWriter.addDataPoint(new LegacyDoubleDataPoint(now + 2, 44.1));

		dataPointWriter.close();

		List<DataPointRow> rows = csResult.getRows();
		assertEquals(1, rows.size());

		DataPointRow row = rows.get(0);

		// Verify first data point (regular)
		assertThat(row.hasNext(), equalTo(true));
		DataPoint dp1 = row.next();
		assertThat(dp1.isLong(), equalTo(true));
		assertThat(dp1.getLongValue(), equalTo(42L));
		assertThat(dp1.getTimestamp(), equalTo(now));
		assertThat(dp1 instanceof IngestionTimestampDataPoint, equalTo(false));

		// Verify second data point (IngestionTimestampDataPoint)
		assertThat(row.hasNext(), equalTo(true));
		DataPoint dp2 = row.next();
		assertThat(dp2 instanceof IngestionTimestampDataPoint, equalTo(true));
		IngestionTimestampDataPoint itdp = (IngestionTimestampDataPoint) dp2;
		assertThat(itdp.isLong(), equalTo(true));
		assertThat(itdp.getLongValue(), equalTo(43L));
		assertThat(itdp.getTimestamp(), equalTo(now + 1));
		assertThat(itdp.getIngestionTimestamp(), equalTo(ingestionTime));

		// Verify third data point (regular)
		assertThat(row.hasNext(), equalTo(true));
		DataPoint dp3 = row.next();
		assertThat(dp3.isDouble(), equalTo(true));
		assertThat(dp3.getDoubleValue(), equalTo(44.1));
		assertThat(dp3.getTimestamp(), equalTo(now + 2));
		assertThat(dp3 instanceof IngestionTimestampDataPoint, equalTo(false));

		assertThat(row.hasNext(), equalTo(false));

		row.close();
		csResult.close();

		// Re-open cached file and verify the data is preserved correctly
		csResult = CachedSearchResult.openCachedSearchResult("metric_ingestion", tempFile, 100, dataPointFactory, true);
		rows = csResult.getRows();
		assertEquals(1, rows.size());

		row = rows.get(0);

		// Verify first data point (regular) after reload
		assertThat(row.hasNext(), equalTo(true));
		dp1 = row.next();
		assertThat(dp1.isLong(), equalTo(true));
		assertThat(dp1.getLongValue(), equalTo(42L));
		assertThat(dp1.getTimestamp(), equalTo(now));
		assertThat(dp1 instanceof IngestionTimestampDataPoint, equalTo(false));

		// Verify second data point (IngestionTimestampDataPoint) after reload
		assertThat(row.hasNext(), equalTo(true));
		dp2 = row.next();
		assertThat(dp2 instanceof IngestionTimestampDataPoint, equalTo(true));
		itdp = (IngestionTimestampDataPoint) dp2;
		assertThat(itdp.isLong(), equalTo(true));
		assertThat(itdp.getLongValue(), equalTo(43L));
		assertThat(itdp.getTimestamp(), equalTo(now + 1));
		assertThat(itdp.getIngestionTimestamp(), equalTo(ingestionTime));

		// Verify third data point (regular) after reload
		assertThat(row.hasNext(), equalTo(true));
		dp3 = row.next();
		assertThat(dp3.isDouble(), equalTo(true));
		assertThat(dp3.getDoubleValue(), equalTo(44.1));
		assertThat(dp3.getTimestamp(), equalTo(now + 2));
		assertThat(dp3 instanceof IngestionTimestampDataPoint, equalTo(false));

		assertThat(row.hasNext(), equalTo(false));

		row.close();
		csResult.close();
	}

	private void assertValues(DataPointRow dataPoints, Number... numbers)
	{
		int count = 0;
		while (dataPoints.hasNext())
		{
			DataPoint dp = dataPoints.next();

			if (dp.isLong())
			{
				Long value = (Long)numbers[count];
				assertEquals(value.longValue(), dp.getLongValue());
			}
			else
			{
				Double value = (Double)numbers[count];
				assertEquals(value, dp.getDoubleValue());
			}

			count ++;
		}
	}

	@Test
	public void test_StringDataPoint_Roundtrip() throws IOException
	{
		String tempFile = System.getProperty("java.io.tmpdir") + "/baseFile_string";
		CachedSearchResult csResult =
				CachedSearchResult.createCachedSearchResult("metric_string", tempFile, dataPointFactory, true);

		long now = System.currentTimeMillis();

		SortedMap<String, String> tags = new TreeMap<>();
		tags.put("host", "A");
		QueryCallback.DataPointWriter dataPointWriter = csResult.startDataPointSet(StringDataPointFactory.DST_STRING, tags);

		String[] testStrings = {
				"Simple string",
				"Unicode: 你好世界 🌍",
				"",
				"Special chars: \t\n\r\"'\\",
		};

		for (int i = 0; i < testStrings.length; i++)
		{
			dataPointWriter.addDataPoint(new StringDataPoint(now + i, testStrings[i]));
		}

		dataPointWriter.close();

		List<DataPointRow> rows = csResult.getRows();
		assertEquals(1, rows.size());

		DataPointRow row = rows.get(0);

		for (int i = 0; i < testStrings.length; i++)
		{
			assertThat(row.hasNext(), equalTo(true));
			DataPoint dp = row.next();
			assertThat(dp instanceof StringDataPoint, equalTo(true));
			StringDataPoint sdp = (StringDataPoint) dp;
			assertThat(sdp.getValue(), equalTo(testStrings[i]));
			assertThat(sdp.getTimestamp(), equalTo(now + i));
		}

		assertThat(row.hasNext(), equalTo(false));
		row.close();
		csResult.close();

		// Re-open cached file and verify data survives disk roundtrip
		csResult = CachedSearchResult.openCachedSearchResult("metric_string", tempFile, 100, dataPointFactory, true);
		rows = csResult.getRows();
		assertEquals(1, rows.size());

		row = rows.get(0);

		for (int i = 0; i < testStrings.length; i++)
		{
			assertThat(row.hasNext(), equalTo(true));
			DataPoint dp = row.next();
			assertThat(dp instanceof StringDataPoint, equalTo(true));
			StringDataPoint sdp = (StringDataPoint) dp;
			assertThat(sdp.getValue(), equalTo(testStrings[i]));
			assertThat(sdp.getTimestamp(), equalTo(now + i));
		}

		assertThat(row.hasNext(), equalTo(false));
		row.close();
		csResult.close();
	}

	@Test
	public void test_StringDataPoint_LargeStrings() throws IOException
	{
		String tempFile = System.getProperty("java.io.tmpdir") + "/baseFile_large_string";
		CachedSearchResult csResult =
				CachedSearchResult.createCachedSearchResult("metric_large_string", tempFile, dataPointFactory, true);

		long now = System.currentTimeMillis();

		SortedMap<String, String> tags = new TreeMap<>();
		tags.put("host", "A");
		QueryCallback.DataPointWriter dataPointWriter = csResult.startDataPointSet(StringDataPointFactory.DST_STRING, tags);

		// Create large strings that exceed the old writeUTF limit (65535 bytes)
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 100000; i++)
		{
			sb.append("A");
		}
		String largeString = sb.toString();  // 100KB string

		// Also test a string with unicode that results in large UTF-8
		StringBuilder unicodeSb = new StringBuilder();
		for (int i = 0; i < 30000; i++)
		{
			unicodeSb.append("日");  // 3 bytes in UTF-8
		}
		String largeUnicodeString = unicodeSb.toString();  // ~90KB in UTF-8

		dataPointWriter.addDataPoint(new StringDataPoint(now, largeString));
		dataPointWriter.addDataPoint(new StringDataPoint(now + 1, largeUnicodeString));
		dataPointWriter.close();

		List<DataPointRow> rows = csResult.getRows();
		assertEquals(1, rows.size());

		DataPointRow row = rows.get(0);

		// Verify large ASCII string
		assertThat(row.hasNext(), equalTo(true));
		DataPoint dp1 = row.next();
		assertThat(dp1 instanceof StringDataPoint, equalTo(true));
		assertThat(((StringDataPoint) dp1).getValue(), equalTo(largeString));

		// Verify large Unicode string
		assertThat(row.hasNext(), equalTo(true));
		DataPoint dp2 = row.next();
		assertThat(dp2 instanceof StringDataPoint, equalTo(true));
		assertThat(((StringDataPoint) dp2).getValue(), equalTo(largeUnicodeString));

		assertThat(row.hasNext(), equalTo(false));
		row.close();
		csResult.close();

		// Re-open cached file and verify large strings survive disk roundtrip
		csResult = CachedSearchResult.openCachedSearchResult("metric_large_string", tempFile, 100, dataPointFactory, true);
		rows = csResult.getRows();
		assertEquals(1, rows.size());

		row = rows.get(0);

		assertThat(row.hasNext(), equalTo(true));
		dp1 = row.next();
		assertThat(((StringDataPoint) dp1).getValue(), equalTo(largeString));

		assertThat(row.hasNext(), equalTo(true));
		dp2 = row.next();
		assertThat(((StringDataPoint) dp2).getValue(), equalTo(largeUnicodeString));

		assertThat(row.hasNext(), equalTo(false));
		row.close();
		csResult.close();
	}

	@Test
	public void test_StringDataPoint_ManyStringsBeyondBufferSize() throws IOException
	{
		String tempFile = System.getProperty("java.io.tmpdir") + "/baseFile_many_strings";
		CachedSearchResult csResult = CachedSearchResult.createCachedSearchResult(
				"metric_many_strings", tempFile, dataPointFactory, true);

		// Create many strings to exceed buffer size and test buffer flushing
		int numberOfDataPoints = CachedSearchResult.WRITE_BUFFER_SIZE / 10;  // Ensure we exceed buffer
		QueryCallback.DataPointWriter dataPointWriter = csResult.startDataPointSet(
				StringDataPointFactory.DST_STRING, Collections.<String, String>emptySortedMap());

		long now = System.currentTimeMillis();
		for (int i = 0; i < numberOfDataPoints; i++)
		{
			dataPointWriter.addDataPoint(new StringDataPoint(now + i, "Value_" + i));
		}

		dataPointWriter.close();

		List<DataPointRow> rows = csResult.getRows();
		DataPointRow row = rows.iterator().next();

		int count = 0;
		while (row.hasNext())
		{
			DataPoint dataPoint = row.next();
			assertThat(dataPoint instanceof StringDataPoint, equalTo(true));
			assertThat(((StringDataPoint) dataPoint).getValue(), equalTo("Value_" + count));
			count++;
		}

		assertThat(count, equalTo(numberOfDataPoints));
		row.close();
		csResult.close();
	}
}
