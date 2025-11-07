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

	@Override
	public String readUTFLong() throws IOException
	{
		int savedPosition = m_buffer.position();
		int totalRemaining = m_buffer.remaining();
		
		if (totalRemaining < 2)
		{
			throw new IOException("Not enough bytes to read string length");
		}
		
		if (totalRemaining >= 4)
		{
			int newLength = readInt();
			
			if (newLength >= 0 && newLength <= Integer.MAX_VALUE - 10)
			{
				int remainingAfterLength = m_buffer.remaining();
				if (remainingAfterLength >= newLength)
				{
					byte[] bytes = new byte[newLength];
					readFully(bytes);
					return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
				}
				m_buffer.position(savedPosition);
			}
			else
			{
				m_buffer.position(savedPosition);
			}
		}
		
		m_buffer.position(savedPosition);
		int oldLength = readUnsignedShort();
		int remainingAfterLength = m_buffer.remaining();
		
		if (oldLength <= 65535 && remainingAfterLength == oldLength)
		{
			byte[] bytes = new byte[oldLength];
			readFully(bytes);
			return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
		}
		else
		{
			m_buffer.position(savedPosition);
			int newLength = readInt();
			
			if (newLength < 0 || newLength > Integer.MAX_VALUE - 10)
			{
				throw new IOException("Invalid string length: " + newLength);
			}
			
			byte[] bytes = new byte[newLength];
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
