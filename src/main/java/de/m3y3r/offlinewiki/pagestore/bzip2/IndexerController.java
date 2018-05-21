package de.m3y3r.offlinewiki.pagestore.bzip2;

import java.io.IOException;

import de.m3y3r.offlinewiki.utility.Bzip2BlockInputStream;
import de.m3y3r.offlinewiki.utility.SplitFile;

public class IndexerController implements Runnable {

	private final SplitFile xmlDumpFile;
	private IndexerEventListener indexerEventListener;

	public IndexerController(SplitFile xmDumpFile) {
		this.xmlDumpFile = xmDumpFile;
	}

	@Override
	public void run() {
//		blockNo=19045 bitPos=34667753469 - 48
//		blockNo=19046 bitPos=34669437966

		long fromBits = 34667753421l;
		long toBits = 34669437966l;

		try (Bzip2BlockInputStream stream = new Bzip2BlockInputStream(xmlDumpFile, fromBits, toBits)) {
			Indexer indexerJob = new Indexer(stream, fromBits);
			indexerJob.addEventListener(indexerEventListener);
			indexerJob.run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void buildRestartStream() {
//		// restart indexing from the last position
//		if(currentChar >= 0 && restarBlockPositionInBits != null) {
//			long posInBits = restarBlockPositionInBits;
//			fis.seek(posInBits / 8); // position underlying file to the bzip2 block start
//			in.clearBuffer(); // clear buffer content
//			bZip2In.resetBlock((byte) (posInBits % 8)); // consume superfluous bits
//
//			// fix internal state of Bzip2CompressorInputStream
//			offsetBlockPositionInBits = restarBlockPositionInBits / 8 * 8; // throw away superfluous bits
//			offsetBlockUncompressedPosition = restartBlockPositionUncompressed;
//
//			// skip to next page; set uncompressed byte position
//			long nextPagePos = restartPagePositionUncompressed - restartBlockPositionUncompressed;
//			bZip2In.skip(nextPagePos);
//			utf8Reader.setCurrentFilePos(restartPagePositionUncompressed);
//			currentChar = utf8Reader.read(); // read first character from bzip2 block
//			// fix-up levelNameMap, we are at a new <page> now, create fake level 0 entry
//			levelNameMap.put(1, new StringBuilder("mediawiki"));
//			level++;
//		}
	}
}

