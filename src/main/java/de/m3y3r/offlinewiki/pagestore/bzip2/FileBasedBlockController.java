package de.m3y3r.offlinewiki.pagestore.bzip2;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

public class FileBasedBlockController implements BlockFinderEventListener, Flushable, Closeable {

	private final List entries;
	private final File blockFile;

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
		entries.clear();
	}

	@Override
	public void close() throws IOException {
		entries.clear();
	}

	public List getEntries() {
		return entries;
	}

}
