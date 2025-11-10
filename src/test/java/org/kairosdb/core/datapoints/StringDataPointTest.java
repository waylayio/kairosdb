package org.kairosdb.core.datapoints;

import org.junit.BeforeClass;
import org.junit.Test;
import org.kairosdb.core.DataPoint;
import org.kairosdb.util.KDataInputStream;
import org.kairosdb.util.KDataOutput;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class StringDataPointTest extends DataPointTestCommon
{
	@BeforeClass
	public static void setup()
	{
		StringDataPointFactory dpFactory = new StringDataPointFactory();
		factory = dpFactory;

		dataPointList.clear();
		dataPointList.add(dpFactory.createDataPoint(1, "Bob"));
		dataPointList.add(dpFactory.createDataPoint(1, "Bob Dog"));
		dataPointList.add(dpFactory.createDataPoint(123, "fo.com"));
		dataPointList.add(dpFactory.createDataPoint(123, "123"));
		dataPointList.add(dpFactory.createDataPoint(1234, "1.2.3.4"));

		sum = 0;
	}

	@Test
	public void test_largeString_justUnder65KB() throws Exception
	{
		// Create a string just under 65KB
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 65500; i++)
		{
			sb.append('A');
		}
		String largeString = sb.toString();
		int utf8Length = largeString.getBytes(StandardCharsets.UTF_8).length;

		StringDataPointFactory factory = new StringDataPointFactory();
		StringDataPoint dataPoint = (StringDataPoint) factory.createDataPoint(1000L, largeString);

		KDataOutput output = new KDataOutput();
		dataPoint.writeValueToBuffer(output);

		KDataInputStream input = new KDataInputStream(
				new ByteArrayInputStream(output.getBytes()));
		DataPoint readDataPoint = factory.getDataPoint(1000L, input);

		assertEquals(dataPoint, readDataPoint);
		assertEquals(largeString, ((StringDataPoint) readDataPoint).getValue());
		assertEquals(utf8Length, ((StringDataPoint) readDataPoint).getValue()
				.getBytes(StandardCharsets.UTF_8).length);
	}

	@Test
	public void test_largeString_exactly65KB() throws Exception
	{
		// Create a string exactly 65535 bytes
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 65535; i++)
		{
			sb.append('A');
		}
		String largeString = sb.toString();
		assertEquals(65535, largeString.getBytes(StandardCharsets.UTF_8).length);

		StringDataPointFactory factory = new StringDataPointFactory();
		StringDataPoint dataPoint = (StringDataPoint) factory.createDataPoint(2000L, largeString);

		KDataOutput output = new KDataOutput();
		dataPoint.writeValueToBuffer(output);

		KDataInputStream input = new KDataInputStream(
				new ByteArrayInputStream(output.getBytes()));
		DataPoint readDataPoint = factory.getDataPoint(2000L, input);

		assertEquals(dataPoint, readDataPoint);
		assertEquals(largeString, ((StringDataPoint) readDataPoint).getValue());
		assertEquals(65535, ((StringDataPoint) readDataPoint).getValue()
				.getBytes(StandardCharsets.UTF_8).length);
	}

	@Test
	public void test_largeString_over65KB() throws Exception
	{
		// Create a string over 65KB (100KB)
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 100000; i++)
		{
			sb.append('A');
		}
		String largeString = sb.toString();
		int utf8Length = largeString.getBytes(StandardCharsets.UTF_8).length;
		assertEquals(100000, utf8Length);

		StringDataPointFactory factory = new StringDataPointFactory();
		StringDataPoint dataPoint = (StringDataPoint) factory.createDataPoint(3000L, largeString);

		KDataOutput output = new KDataOutput();
		dataPoint.writeValueToBuffer(output);

		KDataInputStream input = new KDataInputStream(
				new ByteArrayInputStream(output.getBytes()));
		DataPoint readDataPoint = factory.getDataPoint(3000L, input);

		assertEquals(dataPoint, readDataPoint);
		assertEquals(largeString, ((StringDataPoint) readDataPoint).getValue());
		assertEquals(100000, ((StringDataPoint) readDataPoint).getValue()
				.getBytes(StandardCharsets.UTF_8).length);
	}

	@Test
	public void test_largeString_veryLarge() throws Exception
	{
		// Create a very large string (200KB)
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 200000; i++)
		{
			sb.append('B');
		}
		String largeString = sb.toString();
		int utf8Length = largeString.getBytes(StandardCharsets.UTF_8).length;
		assertEquals(200000, utf8Length);

		StringDataPointFactory factory = new StringDataPointFactory();
		StringDataPoint dataPoint = (StringDataPoint) factory.createDataPoint(4000L, largeString);

		KDataOutput output = new KDataOutput();
		dataPoint.writeValueToBuffer(output);

		KDataInputStream input = new KDataInputStream(
				new ByteArrayInputStream(output.getBytes()));
		DataPoint readDataPoint = factory.getDataPoint(4000L, input);

		assertEquals(dataPoint, readDataPoint);
		assertEquals(largeString, ((StringDataPoint) readDataPoint).getValue());
		assertEquals(200000, ((StringDataPoint) readDataPoint).getValue()
				.getBytes(StandardCharsets.UTF_8).length);
	}

	@Test
	public void test_backwardCompatibility_oldFormat_with_2bytes_length() throws Exception
	{
		String testString = "Hello, World!";
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeUTF(testString);

		StringDataPointFactory factory = new StringDataPointFactory();
		KDataInputStream input = new KDataInputStream(
				new ByteArrayInputStream(baos.toByteArray()));
		DataPoint readDataPoint = factory.getDataPoint(5000L, input);

		assertEquals(testString, ((StringDataPoint) readDataPoint).getValue());
	}

	@Test
	public void test_backwardCompatibility_oldFormat_large() throws Exception
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 50000; i++)
		{
			sb.append('A');
		}
		String testString = sb.toString();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeUTF(testString);

		StringDataPointFactory factory = new StringDataPointFactory();
		KDataInputStream input = new KDataInputStream(
				new ByteArrayInputStream(baos.toByteArray()));
		DataPoint readDataPoint = factory.getDataPoint(6000L, input);

		assertEquals(testString, ((StringDataPoint) readDataPoint).getValue());
	}

	@Test
	public void test_unicodeCharacters() throws Exception
	{
		String testString = "Hello 世界 🌍";
		StringDataPointFactory factory = new StringDataPointFactory();
		StringDataPoint dataPoint = (StringDataPoint) factory.createDataPoint(7000L, testString);

		KDataOutput output = new KDataOutput();
		dataPoint.writeValueToBuffer(output);

		KDataInputStream input = new KDataInputStream(
				new ByteArrayInputStream(output.getBytes()));
		DataPoint readDataPoint = factory.getDataPoint(7000L, input);

		assertEquals(dataPoint, readDataPoint);
		assertEquals(testString, ((StringDataPoint) readDataPoint).getValue());
	}

	@Test
	public void test_fallbackToWriteUTF() throws Exception
	{
		String testString = "Test string";
		StringDataPoint dataPoint = new StringDataPoint(8000L, testString);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dataPoint.writeValueToBuffer(dos);

		// Written with old format
		byte[] bytes = baos.toByteArray();
		// Old format: 2-byte length + UTF-8 bytes
		int expectedLength = 2 + testString.getBytes(StandardCharsets.UTF_8).length;
		assertEquals(expectedLength, bytes.length);

		// Read it back with standard readUTF
		java.io.DataInputStream dis = new java.io.DataInputStream(
				new ByteArrayInputStream(bytes));
		String result = dis.readUTF();
		assertEquals(testString, result);
	}
}
