package org.kairosdb.util;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

/**
 Created by bhawkins on 12/10/13.
 */
public class BufferedDataOutputStream extends DataOutputStream implements LongUTFWriter
{
	private WrappedOutputStream m_wrappedOutputStream;

	public static BufferedDataOutputStream create(RandomAccessFile file, long startPosition)
	{
		WrappedOutputStream outputStream = new WrappedOutputStream(file, startPosition);
		BufferedDataOutputStream ret = new BufferedDataOutputStream(outputStream);
		ret.setWrappedOutputStream(outputStream);

		return ret;
	}

	private BufferedDataOutputStream(WrappedOutputStream outputStream)
	{
		super(new BufferedOutputStream(outputStream));
	}

	private void setWrappedOutputStream(WrappedOutputStream outputStream)
	{
		m_wrappedOutputStream = outputStream;
	}

	public long getPosition()
	{
		return m_wrappedOutputStream.getPosition();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void writeUTFLong(String s) throws IOException
	{
		byte[] utf8Bytes = s.getBytes(StandardCharsets.UTF_8);
		writeShort(0xFFFF);
		writeInt(utf8Bytes.length);
		write(utf8Bytes);
	}

	private static class WrappedOutputStream extends OutputStream
	{
		private FileChannel m_file;
		private long m_position;

		public WrappedOutputStream(RandomAccessFile file, long startPosition)
		{
			m_file = file.getChannel();
			m_position = startPosition;
		}

		public long getPosition()
		{
			return m_position;
		}

		@Override
		public void write(int b) throws IOException
		{
		}

		@Override
		public void write(byte[] src, int offset, int length) throws IOException
		{
			ByteBuffer buffer = ByteBuffer.wrap(src, offset, length);
			while (buffer.hasRemaining())
			{
				int written = m_file.write(buffer, m_position);
				m_position += written;
			}
		}
	}
}
