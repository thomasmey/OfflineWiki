package de.m3y3r.offlinewiki.pagestore.bzip2;

import java.io.BufferedInputStream;
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

import de.m3y3r.offlinewiki.Config;
import de.m3y3r.offlinewiki.pagestore.bzip2.FileBasedBlockController.BlockEntry;
import de.m3y3r.offlinewiki.utility.SplitFile;
import de.m3y3r.offlinewiki.utility.SplitFileInputStream;

/**
 * Scans a BZip2 file for block markers
 *
 * @author thomas
 *
 */
public class BlockFinder implements Runnable {

	private static final long COMPRESSED_MAGIC = 0x314159265359l;
	private long currentMagic;
	private long readCountBits;
	private long blockNo;

	private final List<BlockFinderEventListener> eventListeners;
	private SplitFile fileToScan;

	public static void main(String[] args) throws IOException {
//		String baseName = args[0];

		String baseName = "dewiki-latest-pages-articles.xml.bz2";
		File blockFile = new File(baseName + ".blocks");

		FileBasedBlockController blockController = new FileBasedBlockController(blockFile);
		BlockEntry restart = FileBasedBlockController.getLastEntry(blockFile);

		BlockFinder blockFinder = new BlockFinder(restart, null);
		blockFinder.addEventListener(blockController);

		blockFinder.findBlocks(baseName);
		blockController.close();
	}

	private void findBlocks(String baseName) throws IOException {
		long restartMapPosInBytes = readCountBits / 8;
		while(true) {
			int splitNo = (int) (readCountBits / 8 / Config.SPLIT_SIZE);
			File splitFile = new File(baseName + "." + splitNo);
			if(!splitFile.exists())
				break;

			findBlocks(splitFile.toPath(), splitNo, restartMapPosInBytes);
			restartMapPosInBytes = 0;
		}
	}

	private void findBlocks(Path path, int splitNo, long restartMapPosInBytes) throws IOException {
		long startTime = System.currentTimeMillis();

		int mapSize = (int) Math.pow(2,30);
		FileChannel fc = FileChannel.open(path);
		long totalSize = fc.size();
		System.out.format("split no %d, fileSize %d, mapSize %d\n", splitNo, totalSize, mapSize);

		long currentMapPos = restartMapPosInBytes;
		totalSize -= currentMapPos;

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
		if(diffTime/1000 > 0)
			System.out.format("bytes/second=%d\n", diffPos/(diffTime/1000));
		System.out.format("total time=%ds\n", TimeUnit.MILLISECONDS.toSeconds(diffTime));
	}

	public BlockFinder(BlockEntry restart, SplitFile fileToScan) {
		this.eventListeners = new CopyOnWriteArrayList<>();
		if(restart != null) {
			this.blockNo = restart.blockNo;
			this.readCountBits = restart.readCountBits;
		}
		this.fileToScan = fileToScan;
	}

	public void run() {
		long startTime = System.currentTimeMillis();

		try (SplitFileInputStream in = new SplitFileInputStream(fileToScan, Config.SPLIT_SIZE)){
			in.seek(readCountBits / 8);
			try(BufferedInputStream bin = new BufferedInputStream(in, (int) Math.pow(2, 1))) {
				int b = bin.read();
				while(b >= 0) {
					if(Thread.interrupted())
						return;

					update(b);
					b = bin.read();
				}
			}
			fireEventEndOfFile();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			long endTime = System.currentTimeMillis();
			long diffTime = endTime - startTime;
			long diffPos = readCountBits / 8;
			System.out.println("bytes/second=" + diffPos/(diffTime/1000));
			System.out.println("total time=" + TimeUnit.MILLISECONDS.toSeconds(diffTime));
		}
	}

	private void fireEventEndOfFile() {
		for(BlockFinderEventListener el: eventListeners) {
			EventObject event = new EventObject(this);
			el.onEndOfFile(event);
		}
	}

	private void fireEventNewBlock(long blockNo, long readCountBits) {
		for(BlockFinderEventListener el: eventListeners) {
			EventObject event = new EventObject(this);
			el.onNewBlock(event, blockNo, readCountBits);
		}
	}

	public void update(int b) {
		for(byte bi = 7; bi >= 0; bi--) {
			readCountBits++;
			int cb = (b >> bi) & 1;
			currentMagic = currentMagic << 1 | cb;
			if((currentMagic & 0xff_ff_ff_ff_ff_ffl) == COMPRESSED_MAGIC) {
				fireEventNewBlock(blockNo, readCountBits - 48);
				blockNo++;
			}
		}
	}

	public void addEventListener(BlockFinderEventListener el) {
		eventListeners.add(el);
	}
}

