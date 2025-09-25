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

import org.json.JSONException;
import org.json.JSONWriter;
import org.kairosdb.core.DataPoint;
import org.kairosdb.core.datastore.DataPointGroup;

import java.io.DataOutput;
import java.io.IOException;

/**
 * A wrapper around a DataPoint that also includes ingestion timestamp information
 * retrieved using Cassandra's WRITETIME function.
 */
public class IngestionTimestampDataPoint implements DataPoint
{
	private final DataPoint m_dataPoint;
	private final long m_ingestionTimestamp;

	public IngestionTimestampDataPoint(DataPoint dataPoint, long ingestionTimestamp)
	{
		m_dataPoint = dataPoint;
		m_ingestionTimestamp = ingestionTimestamp;
	}

	public long getIngestionTimestamp()
	{
		return m_ingestionTimestamp;
	}

	// Delegate all DataPoint methods to the wrapped DataPoint
	@Override
	public long getTimestamp()
	{
		return m_dataPoint.getTimestamp();
	}

	@Override
	public void setTimestamp(long timestamp)
	{
		m_dataPoint.setTimestamp(timestamp);
	}

	@Override
	public void writeValueToBuffer(DataOutput buffer) throws IOException
	{
		m_dataPoint.writeValueToBuffer(buffer);
	}

	@Override
	public void writeValueToJson(JSONWriter writer) throws JSONException
	{
		m_dataPoint.writeValueToJson(writer);
		// Add ingestion timestamp as additional array element
		writer.value(m_ingestionTimestamp);
	}

	@Override
	public String getApiDataType()
	{
		return m_dataPoint.getApiDataType();
	}

	@Override
	public String getDataStoreDataType()
	{
		return m_dataPoint.getDataStoreDataType();
	}

	@Override
	public boolean isLong()
	{
		return m_dataPoint.isLong();
	}

	@Override
	public long getLongValue()
	{
		return m_dataPoint.getLongValue();
	}

	@Override
	public boolean isDouble()
	{
		return m_dataPoint.isDouble();
	}

	@Override
	public double getDoubleValue()
	{
		return m_dataPoint.getDoubleValue();
	}

	@Override
	public DataPointGroup getDataPointGroup()
	{
		return m_dataPoint.getDataPointGroup();
	}

	@Override
	public void setDataPointGroup(DataPointGroup dataPointGroup)
	{
		m_dataPoint.setDataPointGroup(dataPointGroup);
	}
}