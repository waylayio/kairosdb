package org.kairosdb.util;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class KDataInputStream extends DataInputStream implements KDataInput
{
	/**
	 Creates a DataInputStream that uses the specified
	 underlying InputStream.
	 */
	public KDataInputStream(InputStream in)
	{
		super(in);
	}

	/**
	 * Reads a string in one of three formats for backward compatibility:
	 * <ul>
	 *   <li>New format: 0xFFFF marker + 4-byte length + raw UTF-8</li>
	 *   <li>Legacy format: 4-byte length (starting with 0x0000) + raw UTF-8</li>
	 *   <li>Old format: 2-byte length + UTF-8 bytes</li>
	 * </ul>
	 */
	@Override
	public String readUTFLong() throws IOException
	{
		int firstTwoBytes = readUnsignedShort();
		
		if (firstTwoBytes == 0xFFFF)
		{
			// New format: 0xFFFF marker + 4-byte length + raw UTF-8
			int length = readInt();
			if (length < 0)
			{
				throw new IOException("Invalid string length: " + length);
			}
			byte[] bytes = new byte[length];
			readFully(bytes);
			return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
		}
		else if (firstTwoBytes == 0x0000)
		{
			// Legacy format: 4-byte length starting with 0x0000
			int nextTwoBytes = readUnsignedShort();
			if (nextTwoBytes == 0)
			{
				return "";
			}
			int length = nextTwoBytes;
			byte[] bytes = new byte[length];
			readFully(bytes);
			return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
		}
		else
		{
			// Old format: 2-byte length + UTF-8 bytes
			byte[] bytes = new byte[firstTwoBytes];
			readFully(bytes);
			return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
		}
	}
}
