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

	@Override
	public String readUTFLong() throws IOException
	{
		mark(65537);
		
		try
		{
			int newLength = readInt();
			
			if (newLength < 0 || newLength > Integer.MAX_VALUE - 10)
			{
				reset();
				int oldLength = readUnsignedShort();
				if (oldLength <= 65535)
				{
					byte[] bytes = new byte[oldLength];
					readFully(bytes);
					return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
				}
				throw new IOException("Invalid string length: " + newLength);
			}
			
			try
			{
				byte[] bytes = new byte[newLength];
				readFully(bytes);
				return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
			}
			catch (IOException e)
			{
				reset();
				int oldLength = readUnsignedShort();
				if (oldLength <= 65535)
				{
					byte[] bytes = new byte[oldLength];
					readFully(bytes);
					return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
				}
				throw new IOException("Failed to read string", e);
			}
		}
		catch (Exception e)
		{
			try
			{
				reset();
			}
			catch (IOException resetException)
			{
				throw new IOException("Failed to read string and reset stream", e);
			}
			
			try
			{
				int oldLength = readUnsignedShort();
				if (oldLength <= 65535)
				{
					byte[] bytes = new byte[oldLength];
					readFully(bytes);
					return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
				}
			}
			catch (Exception oldFormatException)
			{
				throw new IOException("Failed to read string in both formats", e);
			}
			
			throw new IOException("Failed to read string", e);
		}
	}
}
