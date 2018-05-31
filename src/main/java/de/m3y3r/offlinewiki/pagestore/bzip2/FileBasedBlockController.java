package de.m3y3r.offlinewiki.pagestore.bzip2;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;

public class FileBasedBlockController implements BlockFinderEventListener, Flushable, Closeable, Iterator<FileBasedBlockController.BlockEntry> {

	/* blockfindereventlistener state */
	private final List<BlockEntry> entries;
	private final File blockFile;
	private final BlockEntry restart;
	/* blockfindereventlistener state */

	/* iterator state */
	private DataInputStream in;
	private boolean isFinished;
	private volatile int availableBlocks;
	private Object wait = new Object();
	/* iterator state */

	public static class BlockEntry {
		public static final int BLOCK_ENTRY_LEN = 20;
		public long blockNo;
		public long readCountBits;
		private IndexState indexState;
	}

	public static enum IndexState {INITIAL, STARTED, FINISHED};

	public FileBasedBlockController(File target, BlockEntry restart) {
		this.blockFile = target;
		this.entries = new ArrayList<>(32);
		this.restart = restart;
	}

	@Override
	public void onNewBlock(EventObject event, long blockNo, long readCountBits) {
		BlockEntry entry = new BlockEntry();
		entry.blockNo = blockNo;
		entry.readCountBits = readCountBits;

		entries.add(entry);
	}

	@Override
	public void flush() throws IOException {
		//TODO/FIXME: file is truncated in restart of BlockFinder, this should be okay
		try(DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(blockFile, true)))) {
			for(BlockEntry e : entries) {
				int indexState = IndexState.INITIAL.ordinal();
				out.writeLong(e.blockNo);
				out.writeLong(e.readCountBits);
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
		flush();
		isFinished = true;
	}

	public List<BlockEntry> getEntries() {
		return entries;
	}

	@Override
	public boolean hasNext() {
		synchronized (wait) {
			while(availableBlocks == 0) {
				if(isFinished)
					break;

				try {
					wait.wait();
				} catch (InterruptedException e) {
					break;
				}
			}
		}
		return availableBlocks > 0;
	}

	@Override
	public BlockEntry next() {
		if(in == null) {
			try {
				FileInputStream fis = new FileInputStream(blockFile);
				if(restart != null) {
					fis.getChannel().position(restart.blockNo * BlockEntry.BLOCK_ENTRY_LEN);
				}
				in = new DataInputStream(fis);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		availableBlocks--;
		try {
			BlockEntry entry = readBlockEntry(in);
			return entry;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static BlockEntry readBlockEntry(DataInputStream in) throws IOException {
		long blockNo = in.readLong();
		long readCountBits = in.readLong();
		int indexState = in.readInt();
		BlockEntry entry = new BlockEntry();
		entry.blockNo = blockNo;
		entry.readCountBits = readCountBits;
		entry.indexState = IndexState.values()[indexState];
		return entry;
	}

	public static BlockEntry getLastEntry(File blockFile) {
		if(blockFile == null || !blockFile.exists() || !blockFile.isFile() || blockFile.length() < BlockEntry.BLOCK_ENTRY_LEN) return null;

		long lastCompleteEntry = (blockFile.length() / BlockEntry.BLOCK_ENTRY_LEN * BlockEntry.BLOCK_ENTRY_LEN); // 8 + 8 + 4

		try {
			FileChannel fc = FileChannel.open(blockFile.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE);
			fc.truncate(lastCompleteEntry);
			try(FileInputStream fis = new FileInputStream(blockFile)) {
				fis.getChannel().position(lastCompleteEntry - BlockEntry.BLOCK_ENTRY_LEN);
				try(DataInputStream in = new DataInputStream(fis)) {
					return readBlockEntry(in);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
