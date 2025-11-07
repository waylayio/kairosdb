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
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 65535; i++)
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
		assertEquals(65535, result.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
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
}
