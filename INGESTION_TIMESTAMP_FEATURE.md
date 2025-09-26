# Ingestion Timestamp Feature Implementation (WPPM-2808)

## Overview

This feature adds the ability to retrieve the ingestion timestamp (when data was written to Cassandra) alongside the data timestamp using Cassandra's WRITETIME function.

## Implementation Details

### Core Changes

1. **DatastoreMetricQuery Interface** (`DatastoreMetricQuery.java`)
   - Added `isReturnIngestionTimestamp()` method

2. **QueryMetric Class** (`QueryMetric.java`)
   - Added `returnIngestionTimestamp` field
   - Added `setReturnIngestionTimestamp()` and `isReturnIngestionTimestamp()` methods
   - Updated `toString()` method to include the new field

3. **IngestionTimestampDataPoint Class** (New)
   - Wrapper class that extends DataPoint functionality
   - Holds both the original DataPoint and the ingestion timestamp
   - Modifies JSON output to include ingestion timestamp as third array element

4. **CassandraDatastore Changes** (`CassandraDatastore.java`)
   - Modified QueryListener to accept and use the returnIngestionTimestamp flag
   - Updated data point creation to wrap with IngestionTimestampDataPoint when requested
   - Enhanced query logic to choose appropriate prepared statements

5. **ClusterConnection Changes** (`ClusterConnection.java`)
   - Added new CQL queries with WRITETIME function:
     - `DATA_POINTS_QUERY_ASC_WITH_WRITETIME`
     - `DATA_POINTS_QUERY_DESC_WITH_WRITETIME`  
     - `DATA_POINTS_QUERY_ASC_LIMIT_WITH_WRITETIME`
     - `DATA_POINTS_QUERY_DESC_LIMIT_WITH_WRITETIME`
   - Added corresponding PreparedStatement fields
   - Updated statement preparation in `tryToConnect()`

6. **JSON API Support** (`QueryParser.java`)
   - Added `return_ingestion_timestamp` field to Metric class
   - Updated parsing logic to set the flag on QueryMetric objects

### JSON API Usage

To request ingestion timestamps, add `"return_ingestion_timestamp": true` to your metric query:

```json
{
  "start_absolute": 1609459200000,
  "end_absolute": 1609545600000,
  "metrics": [
    {
      "name": "your.metric.name",
      "return_ingestion_timestamp": true,
      "tags": {
        "host": "server01"
      }
    }
  ]
}
```

### Response Format

When `return_ingestion_timestamp` is true, each data point in the response will include three values instead of two:
- `[timestamp, value, ingestion_timestamp]`

Example response:
```json
{
  "queries": [
    {
      "results": [
        {
          "name": "your.metric.name",
          "tags": {"host": "server01"},
          "values": [
            [1609459260000, 42.5, 1609459261234],
            [1609459320000, 43.1, 1609459321456]
          ]
        }
      ]
    }
  ]
}
```

### Notes

- The ingestion timestamp is retrieved using Cassandra's `WRITETIME()` function
- WRITETIME returns microseconds, which are converted to milliseconds for consistency
- The feature is backward compatible - existing queries without the flag work unchanged
- When no ingestion timestamp is available (shouldn't happen in normal operation), the flag is ignored