/*
 * Copyright (C) 2012 Thomas Meyer
 */

package offlineWiki;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;

class Utf8Reader extends Reader {

	private static class Buffer extends InputStream {
		private final byte[] data;
		private final int length;
		private int position;

		public Buffer(byte[] data, int length) {
			super();
			this.data = data;
			this.length = length;
		}

		public int read() {
			assert position >= 0;

			if(position >= length || length < 0)
				return -1;

			int rc = data[position] & 0xff;
			position++;
			return rc;
		}

		public int available() {
			int rem = position - length;
			return rem <=0 ? 0 : rem;
		}
	}

	private static final int BUFFER_SIZE = 1024 * 124;

	private Buffer readBuffer;
	private Future<Buffer> readBufferNextTask;

	private long currenFilePos;
	private final InputStream in;

	public Utf8Reader(InputStream in) {
		this.in = in;
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
		readBufferNextTask.cancel(true);
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

		if(readBuffer== null) {
			byte[] data = new byte[BUFFER_SIZE];
			int length = in.read(data);
			readBuffer = new Buffer(data, length);
			if(readBuffer.available() == 0)
				return rc;
		} else {
			if(readBuffer.available() == 0) {

				if(readBufferNextTask == null) {
					//first call read first buffer data and schedule next
					scheduleReadBufferNext();
				} else {
					try {
						readBuffer = readBufferNextTask.get();
						scheduleReadBufferNext();
					} catch (InterruptedException e) {
						throw new IOException(e);
					} catch (ExecutionException e) {
						OfflineWiki.getInstance().getLogger().log(Level.SEVERE, "Exception in read occured!", e);
						return rc;
					}
				}
			}
		}

		rc = readBuffer.read();
		if(rc < 0)
			return rc;

		if(++currenFilePos % (1024*1024*128) == 0)
			OfflineWiki.getInstance().getLogger().log(Level.FINE,"FilePos: " + currenFilePos);
		return rc;
	}

	private void scheduleReadBufferNext() {
		Callable<Buffer> readNextTask = null;
		readNextTask = new Callable<Buffer>() {

			@Override
			public Buffer call() throws Exception {
				byte[] data = new byte[BUFFER_SIZE];
				int length = in.read(data);
				return new Buffer(data, length);
			}
		};
		readBufferNextTask = OfflineWiki.getInstance().getThreadPool().submit(readNextTask);
	}
}