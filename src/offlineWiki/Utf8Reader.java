/*
 * Copyright (C) 2012 Thomas Meyer
 */

package offlineWiki;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.util.logging.Level;

class Utf8Reader extends Reader {

	private byte[] readBuffer;
	private int readBufferPos;
	private int readBufferLength;

	private long currenFilePos;
	private final RandomAccessFile raf;

	public Utf8Reader(File inputFile) throws FileNotFoundException {
		raf = new RandomAccessFile(inputFile, "r");
	}

	public long getCurrenFilePos() {
		return currenFilePos;
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void close() throws IOException {
		readBuffer = null;
		raf.close();
	}

	// this implements an easy method to get the next UTF-8 char...
	// JAVA Y U N PROVIDE THIS?
	public int read() throws IOException {

		int rc = 0;
		int b = readBuffered();
		if(b < 0)
			return b;

		if((b >> 7 ) == 0) {
			// single byte UTF-8
			rc = (b & 0x7f);
		} else {
			// multi byte
			int mask = 0;
			int count = 0;

			if((b >> 5) == 0x06) {
				mask = 0x1f;
				count = 1;
			} else
				if((b >> 4) == 0xe) {
					mask = 0x0f;
					count = 2;
				} else
					if((b >> 3) == 0x1e) {
						mask = 0x07;
						count = 3;
					}

			rc = b & mask;
			for(int i = 0; i < count; i++) {
				rc = rc << 6;
				b = readBuffered();
				if(b < 0)
					throw new EOFException();
				if((b >> 6) == 2) {
					rc = (rc | (b & 0x3f));
				} else
					throw new IllegalArgumentException("Invalid UTF-8 sequence!");
			}

		}
		return rc;
	}

	// we need to do the buffering ourself
	private int readBuffered() throws IOException {

		int rc = -1;
		if(readBuffer== null)
			readBuffer = new byte[1024*1024*16];

		if(readBufferLength==0 || readBufferPos > readBufferLength - 1) {
			readBufferLength = raf.read(readBuffer);
			readBufferPos=0;
		}
		if(readBufferLength < 0)
			return rc;

		rc = readBuffer[readBufferPos] & 0xff;
		readBufferPos++;
		currenFilePos++;
		if(currenFilePos % (1024*1024*128) == 0)
			OfflineWiki.getInstance().getLogger().log(Level.FINE,"FilePos: " + currenFilePos);
		return rc;
	}
}