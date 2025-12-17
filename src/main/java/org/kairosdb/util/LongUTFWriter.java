package org.kairosdb.util;

import java.io.IOException;

/**
 * Interface for writing strings in extended UTF format that supports strings larger than 65535 bytes
 * and uses standard UTF-8 encoding (not Java's modified UTF-8).
 * 
 * Format: 0xFFFF marker (2 bytes) + length (4 bytes) + raw UTF-8 bytes
 */
public interface LongUTFWriter
{
	/**
	 * Writes a string in extended format for strings > 65535 bytes.
	 * Uses standard UTF-8 encoding, which properly handles all Unicode characters
	 * including 4-byte characters like emojis.
	 *
	 * @param s the string to write
	 * @throws IOException if an I/O error occurs
	 */
	void writeUTFLong(String s) throws IOException;
}

