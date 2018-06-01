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
import java.io.Serializable;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.compress.compressors.lz77support.LZ77Compressor.Block;

public class FileBasedBlockController implements BlockFinderEventListener, Flushable, Closeable {

	public static class FileBasedBlockIterator implements Iterator<FileBasedBlockController.BlockEntry> {
		private DataInputStream in;
		private BlockEntry next;

		private final File blockFile;

		public FileBasedBlockIterator(File blockFile) {
			this.blockFile = blockFile;
		}

		@Override
		public boolean hasNext() {
			if(in == null) {
				try {
					System.out.println("bf len=" + blockFile.length());
					FileInputStream fis = new FileInputStream(blockFile);
					in = new DataInputStream(fis);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(in != null)
				try {
					next = BlockEntry.readObject(in);
					return true;
				} catch (IOException e) {
					in = null;
					e.printStackTrace();
				}
			return false;
		}

		@Override
		public BlockEntry next() {
			return next;
		}

		public void setBlockFinished(long blockNo) {
		}
	}

	public static class BlockEntry implements Serializable {
		private static final long serialVersionUID = 1L;

		public static final int BLOCK_ENTRY_LEN = 20; // 8 + 8 + 4

		public long blockNo;
		public long readCountBits;
		public IndexState indexState;

		public static void writeObject(DataOutputStream out, BlockEntry be) 
				throws IOException {
			out.writeLong(be.blockNo);
			out.writeLong(be.readCountBits);
			out.writeInt(be.indexState.ordinal());
		}

		public static BlockEntry readObject(DataInputStream in) throws IOException { 
			long blockNo = in.readLong();
			long readCountBits = in.readLong();
			int indexState = in.readInt();

			BlockEntry be = new BlockEntry();
			be.blockNo = blockNo;
			be.readCountBits = readCountBits;
			be.indexState = IndexState.values()[indexState];
			return be;
		}

	}

	private final List<BlockEntry> entries;
	private final File blockFile;

	public static enum IndexState {INITIAL, STARTED, FINISHED};

	public FileBasedBlockController(File target) {
		this.blockFile = target;
		this.entries = new ArrayList<>(32);
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
				e.indexState = IndexState.INITIAL;
				BlockEntry.writeObject(out, e);
			}
		}
		entries.clear();
	}

	@Override
	public void close() throws IOException {
		flush();
	}

	public static BlockEntry getLastEntry(File blockFile) {
		if(blockFile == null || !blockFile.exists() || !blockFile.isFile() || blockFile.length() < BlockEntry.BLOCK_ENTRY_LEN) return null;

		long lastCompleteEntry = blockFile.length() / BlockEntry.BLOCK_ENTRY_LEN * BlockEntry.BLOCK_ENTRY_LEN;

		try {
			FileChannel fc = FileChannel.open(blockFile.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE);
			fc.truncate(lastCompleteEntry);
			try(FileInputStream fis = new FileInputStream(blockFile)) {
				fis.getChannel().position(lastCompleteEntry - BlockEntry.BLOCK_ENTRY_LEN);
				try(DataInputStream in = new DataInputStream(fis)) {
					return BlockEntry.readObject(in);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
