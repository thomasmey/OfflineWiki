package de.m3y3r.offlinewiki.pagestore.bzip2;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import de.m3y3r.offlinewiki.pagestore.bzip2.FileBasedBlockController.BlockEntry;
import de.m3y3r.offlinewiki.pagestore.bzip2.FileBasedBlockController.FileBasedBlockIterator;
import de.m3y3r.offlinewiki.pagestore.bzip2.FileBasedBlockController.IndexState;
import de.m3y3r.offlinewiki.utility.Bzip2BlockInputStream;
import de.m3y3r.offlinewiki.utility.SplitFile;

public class IndexerController implements Runnable, Closeable {

	private final SplitFile xmlDumpFile;
	private final IndexerEventListener indexerEventListener;
	private final ExecutorService threadPool;
	private final Iterator<BlockEntry> blockProvider;

	public IndexerController(SplitFile xmDumpFile, IndexerEventListener indexEventListener, Iterator<BlockEntry> blockProvider) {
		this.xmlDumpFile = xmDumpFile;
		this.indexerEventListener = indexEventListener;
		//		this.threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		this.threadPool = new ThreadPoolExecutor(0, Runtime.getRuntime().availableProcessors(),
				0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>(2));
		this.blockProvider = blockProvider;
	}

	@Override
	public void run() {
		BlockEntry fromBits = null;

		boolean blockFinderFinished = false;
		outer:
		while(!blockFinderFinished) {

			// wait a bit
			try {
				Thread.sleep(TimeUnit.SECONDS.toMillis(5));
			} catch (InterruptedException e) {
				return;
			}

			// search first INITAL block, i.e. a block that need indexing
			if(fromBits == null && blockProvider.hasNext()) {
				fromBits = blockProvider.next();
				if(fromBits.indexState != IndexState.INITIAL)
					continue outer;
			}

			while(blockProvider.hasNext()) {
				if(Thread.interrupted())
					return;

				BlockEntry toBits = blockProvider.next();
				try (Bzip2BlockInputStream stream = new Bzip2BlockInputStream(xmlDumpFile, fromBits.readCountBits, toBits.readCountBits + 48)) {
					final BlockEntry be = fromBits;
					Indexer indexerJob = new Indexer(stream, be.readCountBits);
					indexerJob.addEventListener(indexerEventListener);
					IndexerEventListener stopper = new IndexerEventListener() {
						@Override
						public void onPageTagEnd(IndexerEvent event, long currentTagEndPos) {
							if(stream.isOverrun())
								stream.endStream();
						}
						@Override
						public void onPageStart(IndexerEvent event) {}
						@Override
						public void onNewTitle(IndexerEvent event, String title, long pageTagStartPos) {
							System.out.println("title=" + title);
						}
						@Override
						public void onEndOfStream(IndexerEvent event, boolean filePos) {
							System.out.println("commit index state");
							((FileBasedBlockIterator) blockProvider).setBlockFinished(be.blockNo);
						}
					};
					indexerJob.addEventListener(stopper);

					int retryCount = 0;
					while(true) {
						try {
							threadPool.submit(indexerJob);
							break;
						} catch(RejectedExecutionException e) {
							retryCount++;
							try {
								Thread.sleep(TimeUnit.SECONDS.toMillis(5));
							} catch (InterruptedException e1) {
								return;
							}
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				fromBits = toBits;
			}
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

	@Override
	public void close() throws IOException {
		threadPool.shutdown();
	}
}

