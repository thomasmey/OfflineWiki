package de.m3y3r.offlinewiki.pagestore.bzip2;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class FileBasedBlockController implements BlockFinderEventListener, Flushable, Closeable, Iterator<Long> {

	private final List entries;
	private final File blockFile;

	private int iteratorIndex;
	private DataInputStream in;
	private boolean isFinished;
	private volatile int availableBlocks;
	private Object wait = new Object();

	public static enum IndexState {INITIAL, STARTED, FINISHED};

	public FileBasedBlockController(File target) throws FileNotFoundException {
		this.blockFile = target;
		this.entries = new ArrayList<>(32);
	}

	@Override
	public void onNewBlock(EventObject event, long blockNo, long readCountBits) {
		Object[] entry = new Object[] {blockNo, readCountBits};
		entries.add(entry);
	}

	@Override
	public void flush() throws IOException {
		try(DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(blockFile, true)))) {
			for(Object e : entries) {
				Object[] entry = (Object[]) e;
				long blockNo = (long)entry[0];
				long readCountBits = (long)entry[1];
				int indexState = IndexState.INITIAL.ordinal();
				out.writeLong(blockNo);
				out.writeLong(readCountBits);
				out.writeInt(indexState);
			}
		}
		availableBlocks += entries.size();
		entries.clear();
		synchronized (wait) {
			wait.notifyAll();
		}
	}

	@Override
	public void close() throws IOException {
		entries.clear();
		isFinished = true;
	}

	public List getEntries() {
		return entries;
	}

	@Override
	public boolean hasNext() {
		if(!isFinished) {
			synchronized (wait) {
				int i = 0;
				while(availableBlocks == 0) {
					i++;
					try {
						wait.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}

		return availableBlocks > 0;
	}

	@Override
	public Long next() {
		if(in == null) {
			try {
				in = new DataInputStream(new FileInputStream(blockFile));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		availableBlocks--;
		try {
			long blockNo = in.readLong();
			long readCountBits = in.readLong();
			int indexState = in.readInt();
			return readCountBits;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
