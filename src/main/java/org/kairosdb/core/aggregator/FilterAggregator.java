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

import org.kairosdb.core.DataPoint;
import org.kairosdb.core.annotation.FeatureComponent;
import org.kairosdb.core.annotation.FeatureProperty;
import org.kairosdb.core.datapoints.StringDataPoint;
import org.kairosdb.core.datastore.DataPointGroup;
import org.kairosdb.plugin.Aggregator;


@FeatureComponent(
        name = "filter",
        description = "Filters datapoints according to filter operation with a null data point."
)
public class FilterAggregator implements Aggregator {
    public enum FilterOperation {
        LTE, LT, GTE, GT, EQUAL, NE
    }

    public FilterAggregator() {
        m_threshold = 0.0;
    }

    public FilterAggregator(FilterOperation filterop, Object threshold) {
        m_filterop = filterop;
        m_threshold = threshold;
    }

    @FeatureProperty(
            name = "filter_op",
            label = "Filter operation",
            description = "The operation performed for each data point.",
            type = "enum",
            options = {"lte", "lt", "gte", "gt", "equal", "ne"},
            default_value = "equal"
    )
    private FilterOperation m_filterop;

    @FeatureProperty(
            label = "Threshold",
            description = "The value the operation is performed on. If the operation is lt, then a null data point is returned if the data point is less than the threshold."
    )
    private Object m_threshold;

    /**
     * Sets filter operation to apply to data points. Values can be LTE, LE, GTE, GT, NE, or EQUAL.
     *
     * @param filterop
     */
    public void setFilterOp(FilterOperation filterop) {
        m_filterop = filterop;
    }

    public void setThreshold(Object threshold) {
        m_threshold = threshold;
    }

    public DataPointGroup aggregate(DataPointGroup dataPointGroup) {
        return new FilterDataPointAggregator(dataPointGroup);
    }

    public boolean canAggregate(String groupType) {
        return true;
    }

    public String getAggregatedGroupType(String groupType) {
        return groupType;
    }

    private class FilterDataPointAggregator extends AggregatedDataPointGroupWrapper {
        public FilterDataPointAggregator(DataPointGroup innerDataPointGroup) {
            super(innerDataPointGroup);
        }


        private <T> boolean filterOut(FilterOperation op, Comparable<T> value, T threshold) {
            switch (op) {
                case LTE:
                    return value.compareTo(threshold) <= 0;
                case LT:
                    return value.compareTo(threshold) < 0;
                case GTE:
                    return value.compareTo(threshold) >= 0;
                case GT:
                    return value.compareTo(threshold) > 0;
                case EQUAL:
                    return value.compareTo(threshold) == 0;
                case NE:
                    return value.compareTo(threshold) != 0;
                default:
                    return false;

            }
        }

        private boolean areTypesComparable(DataPoint dp, Class thresholdClass) {
            return (String.class.isAssignableFrom(thresholdClass) && StringDataPoint.API_TYPE.equals(dp.getApiDataType())) ||
                    (Number.class.isAssignableFrom(thresholdClass) && !StringDataPoint.API_TYPE.equals(dp.getApiDataType()));
        }

        public boolean hasNext() {
            boolean foundValidDp = false;
            while (!foundValidDp && currentDataPoint != null) {
                boolean filterOutValue = false;
                if (areTypesComparable(currentDataPoint, m_threshold.getClass())) {
                    if (Number.class.isAssignableFrom(m_threshold.getClass())) {
                        filterOutValue = filterOut(m_filterop, currentDataPoint.getDoubleValue(), ((Number) m_threshold).doubleValue());
                    } else if (StringDataPoint.API_TYPE.equals(currentDataPoint.getApiDataType())) {
                        filterOutValue = filterOut(m_filterop, ((StringDataPoint) currentDataPoint).getValue(), m_threshold.toString());
                    }
                } else {
                    // when types are different, we filter consider the datapoint to be non-equal to the threshold, so we just check if the operation is non-equal
                    // for all other operations the datapoint will NOT be filtered out
                    filterOutValue = FilterOperation.NE.equals(m_filterop);
                }
                if (filterOutValue) {
                    moveCurrentDataPoint();
                } else {
                    foundValidDp = true;
                }
            }

            return foundValidDp;
        }

        public DataPoint next() {
            DataPoint ret = currentDataPoint;
            moveCurrentDataPoint();
            return ret;
        }

        private void moveCurrentDataPoint() {
            if (hasNextInternal())
                currentDataPoint = nextInternal();
            else
                currentDataPoint = null;
        }
    }
}
