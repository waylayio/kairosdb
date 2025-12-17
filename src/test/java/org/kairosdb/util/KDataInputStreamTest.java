package org.kairosdb.util;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class KDataInputStreamTest
{
	private static final int MAX_OLD_FORMAT_SIZE = 65535;

	@Test
	public void test_readUTFLong_smallString() throws IOException
	{
		String testString = "Hello, World!";
		KDataOutput output = new KDataOutput();
		output.writeUTFLong(testString);

		KDataInputStream input = new KDataInputStream(
				new ByteArrayInputStream(output.getBytes()));
		String result = input.readUTFLong();
		assertEquals(testString, result);
	}

	@Test
	public void test_readUTFLong_emptyString() throws IOException
	{
		String testString = "";
		KDataOutput output = new KDataOutput();
		output.writeUTFLong(testString);

		KDataInputStream input = new KDataInputStream(
				new ByteArrayInputStream(output.getBytes()));
		String result = input.readUTFLong();
		assertEquals(testString, result);
	}

	@Test
	public void test_readUTFLong_newFormat_justUnder65KB() throws IOException
	{
		// String just under 65KB using new format
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 65500; i++)
		{
			sb.append('A');
		}
		String testString = sb.toString();

		KDataOutput output = new KDataOutput();
		output.writeUTFLong(testString);

		KDataInputStream input = new KDataInputStream(
				new ByteArrayInputStream(output.getBytes()));
		String result = input.readUTFLong();
		assertEquals(testString, result);
	}

	@Test
	public void test_readUTFLong_newFormat_exactly65KB() throws IOException
	{
		// String exactly 65535 bytes
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 65535; i++)
		{
			sb.append('A');
		}
		String testString = sb.toString();

		KDataOutput output = new KDataOutput();
		output.writeUTFLong(testString);

		KDataInputStream input = new KDataInputStream(
				new ByteArrayInputStream(output.getBytes()));
		String result = input.readUTFLong();
		assertEquals(testString, result);
		assertEquals(65535, result.getBytes(StandardCharsets.UTF_8).length);
	}

	@Test
	public void test_readUTFLong_newFormat_over65KB() throws IOException
	{
		// String over 65KB (100KB)
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 100000; i++)
		{
			sb.append('A');
		}
		String testString = sb.toString();

		KDataOutput output = new KDataOutput();
		output.writeUTFLong(testString);

		KDataInputStream input = new KDataInputStream(
				new ByteArrayInputStream(output.getBytes()));
		String result = input.readUTFLong();
		assertEquals(testString, result);
		assertEquals(100000, result.getBytes(StandardCharsets.UTF_8).length);
	}

	@Test
	public void test_readUTFLong_backwardCompatibility_oldFormat() throws IOException
	{
		String testString = "Hello, World!";
		java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeUTF(testString);

		KDataInputStream input = new KDataInputStream(
				new ByteArrayInputStream(baos.toByteArray()));
		String result = input.readUTFLong();
		assertEquals(testString, result);
	}

	@Test
	public void test_readUTFLong_backwardCompatibility_oldFormat_large() throws IOException
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 50000; i++)
		{
			sb.append('A');
		}
		String testString = sb.toString();

		java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeUTF(testString);

		KDataInputStream input = new KDataInputStream(
				new ByteArrayInputStream(baos.toByteArray()));
		String result = input.readUTFLong();
		assertEquals(testString, result);
	}

	@Test
	public void test_readUTFLong_backwardCompatibility_oldFormat_maxSize() throws IOException
	{
		// Use 65534 instead of 65535 to avoid collision with 0xFFFF marker
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 65534; i++)
		{
			sb.append('A');
		}
		String testString = sb.toString();

		java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeUTF(testString);

		KDataInputStream input = new KDataInputStream(
				new ByteArrayInputStream(baos.toByteArray()));
		String result = input.readUTFLong();
		assertEquals(testString, result);
		assertEquals(65534, result.getBytes(StandardCharsets.UTF_8).length);
	}

	@Test
	public void test_readUTFLong_multipleStrings() throws IOException
	{
		KDataOutput output = new KDataOutput();
		output.writeUTFLong("First");
		output.writeUTFLong("Second");
		output.writeUTFLong("Third");

		KDataInputStream input = new KDataInputStream(
				new ByteArrayInputStream(output.getBytes()));
		assertEquals("First", input.readUTFLong());
		assertEquals("Second", input.readUTFLong());
		assertEquals("Third", input.readUTFLong());
	}

	@Test
	public void test_readUTFLong_unicodeCharacters() throws IOException
	{
		String testString = "Hello 世界 🌍";
		KDataOutput output = new KDataOutput();
		output.writeUTFLong(testString);

		KDataInputStream input = new KDataInputStream(
				new ByteArrayInputStream(output.getBytes()));
		String result = input.readUTFLong();
		assertEquals(testString, result);
	}

	@Test
	public void test_readUTFLong_mixedOldAndNewFormat() throws IOException
	{
		java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeUTF("Old format string");

		KDataOutput output = new KDataOutput();
		output.writeUTFLong("New format string");

		byte[] oldBytes = baos.toByteArray();
		byte[] newBytes = output.getBytes();
		byte[] combined = new byte[oldBytes.length + newBytes.length];
		System.arraycopy(oldBytes, 0, combined, 0, oldBytes.length);
		System.arraycopy(newBytes, 0, combined, oldBytes.length, newBytes.length);

		KDataInputStream input = new KDataInputStream(
				new ByteArrayInputStream(combined));
		assertEquals("Old format string", input.readUTFLong());
		assertEquals("New format string", input.readUTFLong());
	}

	@Test
	public void test_readUTFLong_legacyFormat_smallString() throws IOException
	{
		// Simulates legacy format from commit 0866c3fe (4-byte length, no marker)
		// This should be detected via the 0x0000 high bytes in the length
		String testString = "Legacy format test";
		byte[] utf8Bytes = testString.getBytes(StandardCharsets.UTF_8);

		java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeInt(utf8Bytes.length);  // 4-byte length (no marker)
		dos.write(utf8Bytes);

		KDataInputStream input = new KDataInputStream(
				new ByteArrayInputStream(baos.toByteArray()));
		String result = input.readUTFLong();
		assertEquals(testString, result);
	}

	@Test
	public void test_readUTFLong_legacyFormat_emptyString() throws IOException
	{
		// Legacy format with empty string
		java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeInt(0);  // 4-byte length = 0

		KDataInputStream input = new KDataInputStream(
				new ByteArrayInputStream(baos.toByteArray()));
		String result = input.readUTFLong();
		assertEquals("", result);
	}
}

