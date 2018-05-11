package de.m3y3r.offlinewiki.pagestore.bzip2;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * Scans a BZip2 file for block markers
 *
 * @author thomas
 *
 */
public class BlockFinder implements Runnable {

	@Override
	public void run() {
		Path path = Paths.get("C:\\Users\\thomas\\Downloads\\dewiki-latest-pages-articles.xml.bz2");
		try {
			findBlocks(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void findBlocks(Path path) throws IOException {

		final long COMPRESSED_MAGIC = 0x314159265359l;
		long blockNo = 0;
		long readCountBits = 0;
		long startTime = System.currentTimeMillis();

		long currentMagic = 0;
		int mapSize = (int) Math.pow(2,30);
		FileChannel fc = FileChannel.open(path);
		long totalSize = fc.size();
		long currentMapPos = 0;
		long size = Math.min(totalSize, mapSize);
		while(size > 0) {
			MappedByteBuffer mbb = fc.map(MapMode.READ_ONLY, currentMapPos, size);
			while(mbb.hasRemaining()) {
				byte b = mbb.get();
				for(byte bi = 7; bi >= 0; bi--) {
					readCountBits++;
					int cb = (b >> bi) & 1;
					currentMagic = currentMagic << 1 | cb;
					if((currentMagic & 0xff_ff_ff_ff_ff_ffl) == COMPRESSED_MAGIC) {
						blockNo++;
						fireEvent(blockNo, readCountBits);
					}
				}
			}
			currentMapPos += size;
			totalSize -= size;
			size = Math.min(totalSize, mapSize);
		}
		long time = System.currentTimeMillis();
		long diffTime = time - startTime;
		long diffPos = readCountBits / 8;
		System.out.println("bytes/second=" + diffPos/(diffTime/1000));
		System.out.println("total time=" + TimeUnit.MILLISECONDS.toSeconds(diffTime));
	}

	private void fireEvent(long blockNo, long readCountBits) {

	}

	public static void main(String[] args) {
		new BlockFinder().run();
	}
}

