package org.kairosdb.util;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;


/**
 Created by bhawkins on 12/19/16.
 */
public class ByteBufferDataInput implements KDataInput
{
	private final ByteBuffer m_buffer;

	public ByteBufferDataInput(ByteBuffer buffer)
	{
		m_buffer = buffer;
	}

	@Override
	public void readFully(byte[] b) throws IOException
	{
		m_buffer.get(b);
	}

	@Override
	public void readFully(byte[] b, int off, int len) throws IOException
	{
		m_buffer.get(b, off, len);
	}

	@Override
	public int skipBytes(int n) throws IOException
	{
		int i= 0;
		for (; i < n; i++)
		{
			if (m_buffer.hasRemaining())
				m_buffer.get();
			else
				break;
		}

		return i;
	}

	@Override
	public boolean readBoolean() throws IOException
	{
		return m_buffer.get() != 0;
	}

	@Override
	public byte readByte() throws IOException
	{
		return m_buffer.get();
	}

	@Override
	public int readUnsignedByte() throws IOException
	{
		byte b = m_buffer.get();
		return (b & 0xff);
	}

	@Override
	public short readShort() throws IOException
	{
		return m_buffer.getShort();
	}

	@Override
	public int readUnsignedShort() throws IOException
	{
		return (m_buffer.getShort() & 0xffff);
	}

	@Override
	public char readChar() throws IOException
	{
		return m_buffer.getChar();
	}

	@Override
	public int readInt() throws IOException
	{
		return m_buffer.getInt();
	}

	@Override
	public long readLong() throws IOException
	{
		return m_buffer.getLong();
	}

	@Override
	public float readFloat() throws IOException
	{
		return m_buffer.getFloat();
	}

	@Override
	public double readDouble() throws IOException
	{
		return m_buffer.getDouble();
	}

	@Override
	public String readLine() throws IOException
	{
		return null;
	}

	@Override
	public String readUTF() throws IOException
	{
		return DataInputStream.readUTF(this);
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
		if (m_buffer.remaining() < 2)
		{
			throw new IOException("Not enough bytes to read string length");
		}
		
		int savedPosition = m_buffer.position();
		int firstTwoBytes = readUnsignedShort();
		
		if (firstTwoBytes == 0xFFFF)
		{
			// Could be new format marker OR old format with length=65535
			if (m_buffer.remaining() >= 4)
			{
				int possibleLength = readInt();
				if (possibleLength >= 0 && possibleLength <= m_buffer.remaining())
				{
					// New format: marker + 4-byte length + raw UTF-8
					byte[] bytes = new byte[possibleLength];
					readFully(bytes);
					return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
				}
			}
			// Fall back to old format: 0xFFFF means length=65535
			m_buffer.position(savedPosition + 2);
			byte[] bytes = new byte[65535];
			readFully(bytes);
			return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
		}
		else if (firstTwoBytes == 0x0000)
		{
			// Could be legacy format OR old format empty string
			if (m_buffer.remaining() >= 2)
			{
				int nextTwoBytes = readUnsignedShort();
				if (nextTwoBytes == 0)
				{
					// Empty string (works for both legacy and old format)
					return "";
				}
				if (nextTwoBytes <= m_buffer.remaining())
				{
					// Legacy format: 4-byte length (0x0000 + nextTwoBytes)
					byte[] bytes = new byte[nextTwoBytes];
					readFully(bytes);
					return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
				}
				// Length doesn't fit, revert and treat as old format
				m_buffer.position(savedPosition + 2);
			}
			// Old format empty string
			return "";
		}
		else
		{
			// Old format: 2-byte length + UTF-8 bytes
			byte[] bytes = new byte[firstTwoBytes];
			readFully(bytes);
			return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
		}
	}

	/**
	 Reads data from internal ByteBuffer and writes them to b.  Returns the number of
	 bytes read.
	 @param b
	 @return
	 @throws IOException
	 */
	@Override
	public int read(byte[] b) throws IOException
	{
		int ret = 0;
		if (b.length < m_buffer.remaining())
		{
			ret = b.length;
			m_buffer.get(b);
		}
		else
		{
			ret = m_buffer.remaining();
			m_buffer.get(b, 0, m_buffer.remaining());
		}

		return ret;
	}
}
