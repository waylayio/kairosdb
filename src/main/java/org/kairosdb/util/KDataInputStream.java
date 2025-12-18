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
	 *   <li>Old format: 2-byte length + UTF-8 bytes (from DataOutputStream.writeUTF)</li>
	 * </ul>
	 */
	@Override
	public String readUTFLong() throws IOException
	{
		mark(70000); // Mark to allow reset if format detection fails
		int firstTwoBytes = readUnsignedShort();
		
		if (firstTwoBytes == 0xFFFF)
		{
			// Could be new format marker OR old format with length=65535
			try
			{
				int possibleLength = readInt();
				if (possibleLength >= 0 && possibleLength <= 10_000_000) // Sanity check
				{
					byte[] bytes = new byte[possibleLength];
					readFully(bytes);
					return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
				}
			}
			catch (Exception e)
			{
				// Fall through to old format handling
			}
			// Fall back to old format: 0xFFFF means length=65535
			reset();
			readUnsignedShort(); // Skip the 0xFFFF we already read
			byte[] bytes = new byte[65535];
			readFully(bytes);
			return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
		}
		else if (firstTwoBytes == 0x0000)
		{
			// Could be legacy format OR old format empty string
			try
			{
				int nextTwoBytes = readUnsignedShort();
				if (nextTwoBytes == 0)
				{
					// Empty string (works for both legacy and old format)
					return "";
				}
				// Legacy format: 4-byte length (0x0000 + nextTwoBytes)
				byte[] bytes = new byte[nextTwoBytes];
				readFully(bytes);
				return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
			}
			catch (Exception e)
			{
				// Not enough bytes - must be old format empty string
				reset();
				readUnsignedShort(); // Skip the 0x0000
				return "";
			}
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
