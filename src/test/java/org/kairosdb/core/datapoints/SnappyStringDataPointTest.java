package org.kairosdb.core.datapoints;

import org.junit.Assume;
import org.junit.BeforeClass;

public class SnappyStringDataPointTest extends DataPointTestCommon
{
	private static final boolean IS_MAC_AARCH64;
	
	static {
		String osName = System.getProperty("os.name", "").toLowerCase();
		String osArch = System.getProperty("os.arch", "").toLowerCase();
		IS_MAC_AARCH64 = osName.contains("mac") && osArch.contains("aarch64");
	}
	
	@BeforeClass
	public static void setup()
	{
		Assume.assumeFalse("Snappy native library not available on Mac aarch64", IS_MAC_AARCH64);

		SnappyStringDataPointFactory dpFactory = new SnappyStringDataPointFactory();
		factory = dpFactory;

		dataPointList.clear();
		dataPointList.add(dpFactory.createDataPoint(1, "Bob"));
		dataPointList.add(dpFactory.createDataPoint(1, "Bob Dog"));
		dataPointList.add(dpFactory.createDataPoint(123, "fo.com"));
		dataPointList.add(dpFactory.createDataPoint(123, "123"));
		dataPointList.add(dpFactory.createDataPoint(1234, "1.2.3.4 This is a long string that will get compressed by snappy :)"));

		sum = 0;
	}
}
