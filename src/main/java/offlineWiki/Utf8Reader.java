/*
 * Copyright 2012 Thomas Meyer
 */

package offlineWiki;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;

public class Utf8Reader extends Reader {

	private static int BUFFER_SIZE = 1024 * 1024;
	private final ByteBuffer byteBuffer;

	private long currentFilePos;
	private final InputStream in;
	private boolean byteBufferEOF;

	public Utf8Reader(InputStream in) {

		this.in = in;
		this.byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		fillBuffer();
	}

	public long getCurrenFilePos() {
		return currentFilePos;
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void close() throws IOException {
//		readBuffer = null;
//		if(readBufferNextTask != null)
//			readBufferNextTask.cancel(true);
		in.close();
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
			} else if((b >> 4) == 0xe) {
				mask = 0x0f;
				count = 2;
			} else if((b >> 3) == 0x1e) {
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

	private void fillBuffer() {

		byteBuffer.rewind();
		byte[] ba = byteBuffer.array();

		int len = 0;
		try {
			len = in.read(ba);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if(len < 0) {
			byteBufferEOF = true;
		}
		else {
			byteBuffer.limit(len);
		}
	}

	// we need to do the buffering ourself
	private int readBuffered() throws IOException {

		int rc = -1;
		while(true) {
			if(byteBuffer.hasRemaining()) {
				rc = byteBuffer.get() & 0xff;
				break;
			} else {
				if(!byteBufferEOF)
					fillBuffer();
				else
					return rc;
			}
		}

		currentFilePos++;
		return rc;
	}
}