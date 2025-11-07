package org.kairosdb.util;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

public class KDataOutputTest
{
	private static final int MAX_OLD_FORMAT_SIZE = 65535;
	@Test
	public void test_writeUTFLong_smallString() throws IOException
	{
		String testString = "Hello, World!";
		KDataOutput output = new KDataOutput();
		output.writeUTFLong(testString);

		byte[] bytes = output.getBytes();
		// 4 bytes (int length) + UTF-8 encoded string bytes
		int expectedLength = 4 + testString.getBytes(StandardCharsets.UTF_8).length;
		assertEquals(expectedLength, bytes.length);

		KDataInput input = KDataInput.createInput(bytes);
		String result = input.readUTFLong();
		assertEquals(testString, result);
	}

	@Test
	public void test_writeUTFLong_emptyString() throws IOException
	{
		String testString = "";
		KDataOutput output = new KDataOutput();
		output.writeUTFLong(testString);

		byte[] bytes = output.getBytes();
		// 4 bytes (int length) + 0 bytes
		assertEquals(4, bytes.length);

		KDataInput input = KDataInput.createInput(bytes);
		String result = input.readUTFLong();
		assertEquals(testString, result);
	}

	@Test
	public void test_writeUTFLong_justUnder65KB() throws IOException
	{
		// String just under 65KB (65535 bytes)
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 65500; i++)
		{
			sb.append('A');
		}
		String testString = sb.toString();
		int utf8Length = testString.getBytes(StandardCharsets.UTF_8).length;

		KDataOutput output = new KDataOutput();
		output.writeUTFLong(testString);

		byte[] bytes = output.getBytes();
		// 4 bytes (int length) + UTF-8 encoded string bytes
		int expectedLength = 4 + utf8Length;
		assertEquals(expectedLength, bytes.length);

		KDataInput input = KDataInput.createInput(bytes);
		String result = input.readUTFLong();
		assertEquals(testString, result);
		assertEquals(utf8Length, result.getBytes(StandardCharsets.UTF_8).length);
	}

	@Test
	public void test_writeUTFLong_exactly65KB() throws IOException
	{
		// String that is exactly 65535 bytes when UTF-8 encoded
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 65535; i++)
		{
			sb.append('A');
		}
		String testString = sb.toString();
		int utf8Length = testString.getBytes(StandardCharsets.UTF_8).length;
		assertEquals(65535, utf8Length);

		KDataOutput output = new KDataOutput();
		output.writeUTFLong(testString);

		byte[] bytes = output.getBytes();
		// 4 bytes (int length) + 65535 bytes
		assertEquals(4 + 65535, bytes.length);

		KDataInput input = KDataInput.createInput(bytes);
		String result = input.readUTFLong();
		assertEquals(testString, result);
		assertEquals(65535, result.getBytes(StandardCharsets.UTF_8).length);
	}

	@Test
	public void test_writeUTFLong_over65KB() throws IOException
	{
		// String over 65KB (100KB)
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 100000; i++)
		{
			sb.append('A');
		}
		String testString = sb.toString();
		int utf8Length = testString.getBytes(StandardCharsets.UTF_8).length;
		assertEquals(100000, utf8Length);

		KDataOutput output = new KDataOutput();
		output.writeUTFLong(testString);

		byte[] bytes = output.getBytes();
		// 4 bytes (int length) + 100000 bytes
		assertEquals(4 + 100000, bytes.length);

		KDataInput input = KDataInput.createInput(bytes);
		String result = input.readUTFLong();
		assertEquals(testString, result);
		assertEquals(100000, result.getBytes(StandardCharsets.UTF_8).length);
	}

	@Test
	public void test_writeUTFLong_veryLargeString() throws IOException
	{
		// Very large string (200KB)
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 200000; i++)
		{
			sb.append('B');
		}
		String testString = sb.toString();
		int utf8Length = testString.getBytes(StandardCharsets.UTF_8).length;
		assertEquals(200000, utf8Length);

		KDataOutput output = new KDataOutput();
		output.writeUTFLong(testString);

		byte[] bytes = output.getBytes();
		// 4 bytes (int length) + 200000 bytes
		assertEquals(4 + 200000, bytes.length);

		KDataInput input = KDataInput.createInput(bytes);
		String result = input.readUTFLong();
		assertEquals(testString, result);
		assertEquals(200000, result.getBytes(StandardCharsets.UTF_8).length);
	}

	@Test
	public void test_writeUTFLong_unicodeCharacters() throws IOException
	{
		String testString = "Hello 世界 🌍";
		KDataOutput output = new KDataOutput();
		output.writeUTFLong(testString);

		byte[] bytes = output.getBytes();
		KDataInput input = KDataInput.createInput(bytes);
		String result = input.readUTFLong();
		assertEquals(testString, result);
	}

	@Test
	public void test_writeUTFLong_largeUnicodeString() throws IOException
	{
		// Large string with Unicode characters
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 50000; i++)
		{
			sb.append("测试");
		}
		String testString = sb.toString();
		int utf8Length = testString.getBytes(StandardCharsets.UTF_8).length;

		KDataOutput output = new KDataOutput();
		output.writeUTFLong(testString);

		byte[] bytes = output.getBytes();
		assertEquals(4 + utf8Length, bytes.length);

		KDataInput input = KDataInput.createInput(bytes);
		String result = input.readUTFLong();
		assertEquals(testString, result);
		assertEquals(utf8Length, result.getBytes(StandardCharsets.UTF_8).length);
	}

	@Test
	public void test_writeUTFLong_multipleStrings() throws IOException
	{
		KDataOutput output = new KDataOutput();
		output.writeUTFLong("First");
		output.writeUTFLong("Second");
		output.writeUTFLong("Third");

		byte[] bytes = output.getBytes();
		KDataInput input = KDataInput.createInput(bytes);
		assertEquals("First", input.readUTFLong());
		assertEquals("Second", input.readUTFLong());
		assertEquals("Third", input.readUTFLong());
	}

	@Test
	public void test_writeUTFLong_formatVerification() throws IOException
	{
		// 4-byte int length prefix followed by UTF-8 bytes
		String testString = "Test";
		KDataOutput output = new KDataOutput();
		output.writeUTFLong(testString);

		byte[] bytes = output.getBytes();
		byte[] expectedUtf8 = testString.getBytes(StandardCharsets.UTF_8);

		// First 4 bytes should be the length as int
		int length = ((bytes[0] & 0xFF) << 24) |
				((bytes[1] & 0xFF) << 16) |
				((bytes[2] & 0xFF) << 8) |
				(bytes[3] & 0xFF);
		assertEquals(expectedUtf8.length, length);

		// Remaining bytes should be the UTF-8 encoded string
		byte[] actualUtf8 = new byte[expectedUtf8.length];
		System.arraycopy(bytes, 4, actualUtf8, 0, expectedUtf8.length);
		assertArrayEquals(expectedUtf8, actualUtf8);
	}
}

