package de.m3y3r.offlinewiki.pagestore.bzip2;

import java.io.File;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Path;
import java.util.EventObject;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Scans a BZip2 file for block markers
 *
 * @author thomas
 *
 */
public class BlockFinder {

	public static void main(String[] args) throws IOException {
		File inputFile = new File(args[0]);
		BlockFinder blockFinder = new BlockFinder();
		File blockFile = new File(inputFile.getParent(), inputFile.getName() + ".blocks");
		BlockFinderEventListener el = new FileBasedBlockController(blockFile);
		blockFinder.addEventListener(el);
		blockFinder.findBlocks(inputFile.toPath());
	}

	private final List<BlockFinderEventListener> eventListeners;

	public BlockFinder() {
		this.eventListeners = new CopyOnWriteArrayList<>();
	}

	private void findBlocks(Path path) throws IOException {
		long startTime = System.currentTimeMillis();

		int mapSize = (int) Math.pow(2,30);
		FileChannel fc = FileChannel.open(path);
		long totalSize = fc.size();
		long currentMapPos = 0;
		long size = Math.min(totalSize, mapSize);
		while(size > 0) {
			MappedByteBuffer mbb = fc.map(MapMode.READ_ONLY, currentMapPos, size);
			while(mbb.hasRemaining()) {
				byte b = mbb.get();
				update(b);
			}
			currentMapPos += size;
			totalSize -= size;
			size = Math.min(totalSize, mapSize);
		}
		long endTime = System.currentTimeMillis();
		long diffTime = endTime - startTime;
		long diffPos = readCountBits / 8;
		System.out.println("bytes/second=" + diffPos/(diffTime/1000));
		System.out.println("total time=" + TimeUnit.MILLISECONDS.toSeconds(diffTime));
	}

	private void fireEvent(long blockNo, long readCountBits) {
		for(BlockFinderEventListener el: eventListeners) {
			EventObject event = new EventObject(this);
			el.onNewBlock(event, blockNo, readCountBits);
		}
	}

	private static final long COMPRESSED_MAGIC = 0x314159265359l;
	private long currentMagic;
	private long readCountBits;
	private long blockNo;

	public void update(int b) {
		for(byte bi = 7; bi >= 0; bi--) {
			readCountBits++;
			int cb = (b >> bi) & 1;
			currentMagic = currentMagic << 1 | cb;
			if((currentMagic & 0xff_ff_ff_ff_ff_ffl) == COMPRESSED_MAGIC) {
				fireEvent(blockNo, readCountBits - 48);
				blockNo++;
			}
		}
	}

	public void addEventListener(BlockFinderEventListener el) {
		eventListeners.add(el);
	}
}

