package org.kairosdb.util;

import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;

public class ByteBufferDataInputTest
{
	@Test
	public void test_readUnsignedShort42() throws IOException
	{
		ByteBuffer buf = ByteBuffer.allocate(2);
		buf.putShort((short)42);
		buf.rewind();

		ByteBufferDataInput dataInput = new ByteBufferDataInput(buf);
		assertEquals(42, dataInput.readUnsignedShort());
	}

	@Test
	public void test_readUnsignedShort255() throws IOException
	{
		ByteBuffer buf = ByteBuffer.allocate(2);
		buf.putShort((short)255);
		buf.rewind();

		ByteBufferDataInput dataInput = new ByteBufferDataInput(buf);
		assertEquals(255, dataInput.readUnsignedShort());
	}

	@Test
	public void test_readUnsignedShort1024() throws IOException
	{
		ByteBuffer buf = ByteBuffer.allocate(2);
		buf.putShort((short)1024);
		buf.rewind();

		ByteBufferDataInput dataInput = new ByteBufferDataInput(buf);
		assertEquals(1024, dataInput.readUnsignedShort());
	}

	@Test
	public void test_readUnsignedShort32767() throws IOException
	{
		ByteBuffer buf = ByteBuffer.allocate(2);
		buf.putShort((short)32767);
		buf.rewind();

		ByteBufferDataInput dataInput = new ByteBufferDataInput(buf);
		assertEquals(32767, dataInput.readUnsignedShort());
	}

	@Test
	public void test_readUnsignedShort65000() throws IOException
	{
		ByteBuffer buf = ByteBuffer.allocate(2);
		buf.putShort((short)65000);
		buf.rewind();

		ByteBufferDataInput dataInput = new ByteBufferDataInput(buf);
		assertEquals(65000, dataInput.readUnsignedShort());
	}

	@Test
	public void test_readUTFLong_smallString() throws IOException
	{
		String testString = "Hello, World!";
		KDataOutput output = new KDataOutput();
		output.writeUTFLong(testString);

		ByteBufferDataInput dataInput = new ByteBufferDataInput(
				ByteBuffer.wrap(output.getBytes()));
		String result = dataInput.readUTFLong();
		assertEquals(testString, result);
	}

	@Test
	public void test_readUTFLong_emptyString() throws IOException
	{
		String testString = "";
		KDataOutput output = new KDataOutput();
		output.writeUTFLong(testString);

		ByteBufferDataInput dataInput = new ByteBufferDataInput(
				ByteBuffer.wrap(output.getBytes()));
		String result = dataInput.readUTFLong();
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

		ByteBufferDataInput dataInput = new ByteBufferDataInput(
				ByteBuffer.wrap(output.getBytes()));
		String result = dataInput.readUTFLong();
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

		ByteBufferDataInput dataInput = new ByteBufferDataInput(
				ByteBuffer.wrap(output.getBytes()));
		String result = dataInput.readUTFLong();
		assertEquals(testString, result);
		assertEquals(65535, result.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
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

		ByteBufferDataInput dataInput = new ByteBufferDataInput(
				ByteBuffer.wrap(output.getBytes()));
		String result = dataInput.readUTFLong();
		assertEquals(testString, result);
		assertEquals(100000, result.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
	}

	@Test
	public void test_readUTFLong_backwardCompatibility_oldFormat() throws IOException
	{
		String testString = "Hello, World!";
		java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
		java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);
		dos.writeUTF(testString);

		ByteBufferDataInput dataInput = new ByteBufferDataInput(
				ByteBuffer.wrap(baos.toByteArray()));
		String result = dataInput.readUTFLong();
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
		java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);
		dos.writeUTF(testString);

		ByteBufferDataInput dataInput = new ByteBufferDataInput(
				ByteBuffer.wrap(baos.toByteArray()));
		String result = dataInput.readUTFLong();
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
		java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);
		dos.writeUTF(testString);

		ByteBufferDataInput dataInput = new ByteBufferDataInput(
				ByteBuffer.wrap(baos.toByteArray()));
		String result = dataInput.readUTFLong();
		assertEquals(testString, result);
		assertEquals(65534, result.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
	}

	@Test
	public void test_readUTFLong_multipleStrings() throws IOException
	{
		KDataOutput output = new KDataOutput();
		output.writeUTFLong("First");
		output.writeUTFLong("Second");
		output.writeUTFLong("Third");

		ByteBufferDataInput dataInput = new ByteBufferDataInput(
				ByteBuffer.wrap(output.getBytes()));
		assertEquals("First", dataInput.readUTFLong());
		assertEquals("Second", dataInput.readUTFLong());
		assertEquals("Third", dataInput.readUTFLong());
	}

	@Test
	public void test_readUTFLong_unicodeCharacters() throws IOException
	{
		String testString = "Hello 世界 🌍";
		KDataOutput output = new KDataOutput();
		output.writeUTFLong(testString);

		ByteBufferDataInput dataInput = new ByteBufferDataInput(
				ByteBuffer.wrap(output.getBytes()));
		String result = dataInput.readUTFLong();
		assertEquals(testString, result);
	}

	@Test
	public void test_readUTFLong_legacyFormat_smallString() throws IOException
	{
		// Simulates legacy format from commit 0866c3fe (4-byte length, no marker)
		String testString = "Legacy format test";
		byte[] utf8Bytes = testString.getBytes(java.nio.charset.StandardCharsets.UTF_8);

		java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
		java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);
		dos.writeInt(utf8Bytes.length);  // 4-byte length (no marker)
		dos.write(utf8Bytes);

		ByteBufferDataInput dataInput = new ByteBufferDataInput(
				ByteBuffer.wrap(baos.toByteArray()));
		String result = dataInput.readUTFLong();
		assertEquals(testString, result);
	}

	@Test
	public void test_readUTFLong_legacyFormat_emptyString() throws IOException
	{
		// Legacy format with empty string
		java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
		java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);
		dos.writeInt(0);  // 4-byte length = 0

		ByteBufferDataInput dataInput = new ByteBufferDataInput(
				ByteBuffer.wrap(baos.toByteArray()));
		String result = dataInput.readUTFLong();
		assertEquals("", result);
	}

	@Test
	public void test_readUTFLong_oldFormat_emptyString() throws IOException
	{
		// Old format empty string - just 2 bytes: 0x0000
		// This tests the edge case where old writeUTF empty string could be
		// confused with legacy format (which also starts with 0x0000)
		java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
		java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);
		dos.writeUTF("");  // Old format: 2-byte length = 0

		ByteBufferDataInput dataInput = new ByteBufferDataInput(
				ByteBuffer.wrap(baos.toByteArray()));
		String result = dataInput.readUTFLong();
		assertEquals("", result);
	}

	@Test
	public void test_readUTFLong_oldFormat_maxLength65535() throws IOException
	{
		// Old format with exactly 65535 bytes - length is 0xFFFF
		// This tests the edge case where 0xFFFF length could be confused with new format marker
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 65535; i++)
		{
			sb.append('A');
		}
		String testString = sb.toString();

		java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
		java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);
		dos.writeUTF(testString);  // Old format with 0xFFFF length

		ByteBufferDataInput dataInput = new ByteBufferDataInput(
				ByteBuffer.wrap(baos.toByteArray()));
		String result = dataInput.readUTFLong();
		assertEquals(testString, result);
		assertEquals(65535, result.length());
	}

	@Test
	public void test_readUTFLong_mixedFormats_allThree() throws IOException
	{
		// Test reading all three formats in sequence
		java.io.ByteArrayOutputStream combined = new java.io.ByteArrayOutputStream();

		// 1. Old format string
		java.io.ByteArrayOutputStream oldFormatBaos = new java.io.ByteArrayOutputStream();
		java.io.DataOutputStream oldDos = new java.io.DataOutputStream(oldFormatBaos);
		oldDos.writeUTF("Old format");
		combined.write(oldFormatBaos.toByteArray());

		// 2. Legacy format string (4-byte length, no marker)
		java.io.ByteArrayOutputStream legacyBaos = new java.io.ByteArrayOutputStream();
		java.io.DataOutputStream legacyDos = new java.io.DataOutputStream(legacyBaos);
		byte[] legacyBytes = "Legacy format".getBytes(java.nio.charset.StandardCharsets.UTF_8);
		legacyDos.writeInt(legacyBytes.length);
		legacyDos.write(legacyBytes);
		combined.write(legacyBaos.toByteArray());

		// 3. New format string (0xFFFF marker + 4-byte length)
		KDataOutput newOutput = new KDataOutput();
		newOutput.writeUTFLong("New format");
		combined.write(newOutput.getBytes());

		ByteBufferDataInput dataInput = new ByteBufferDataInput(
				ByteBuffer.wrap(combined.toByteArray()));
		assertEquals("Old format", dataInput.readUTFLong());
		assertEquals("Legacy format", dataInput.readUTFLong());
		assertEquals("New format", dataInput.readUTFLong());
	}

	@Test
	public void test_readUTFLong_legacyFormat_largeString() throws IOException
	{
		// Legacy format with large string
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 10000; i++)
		{
			sb.append('X');
		}
		String testString = sb.toString();
		byte[] utf8Bytes = testString.getBytes(java.nio.charset.StandardCharsets.UTF_8);

		java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
		java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);
		dos.writeInt(utf8Bytes.length);  // 4-byte length
		dos.write(utf8Bytes);

		ByteBufferDataInput dataInput = new ByteBufferDataInput(
				ByteBuffer.wrap(baos.toByteArray()));
		String result = dataInput.readUTFLong();
		assertEquals(testString, result);
	}
}
