package de.m3y3r.offlinewiki.pagestore.bzip2.index;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import de.m3y3r.offlinewiki.pagestore.bzip2.BlockEntry;
import de.m3y3r.offlinewiki.pagestore.bzip2.BlockEntry.IndexState;
import de.m3y3r.offlinewiki.pagestore.bzip2.blocks.BlockController;
import de.m3y3r.offlinewiki.utility.Bzip2BlockInputStream;
import de.m3y3r.offlinewiki.utility.SplitFile;

public class IndexerController implements Runnable, Closeable {

	private final SplitFile xmlDumpFile;
	private final IndexerEventListener indexerEventListener;
	private final ExecutorService threadPool;
	private final BlockController blockController;

	public IndexerController(SplitFile xmDumpFile, IndexerEventListener indexEventListener, BlockController blockController) {
		this.xmlDumpFile = xmDumpFile;
		this.indexerEventListener = indexEventListener;
		this.blockController = blockController;
//		int noThreads = Runtime.getRuntime().availableProcessors();
		int noThreads = 1;
//		this.threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		this.threadPool = new ThreadPoolExecutor(0, noThreads,
				0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>(2));
	}

	@Override
	public void run() {
		boolean blockFinderFinished = false;
		BlockEntry currentBits = null;

		Iterator<BlockEntry> blockProvider = blockController.getBlockIterator();
		outer:
			while(!blockFinderFinished) {
				while(blockProvider.hasNext()) {
					if(Thread.interrupted())
						return;

					currentBits = blockProvider.next();
					// search first INITAL block, i.e. a block that need indexing
					if(currentBits != null) {
						//FIXME:
						if(currentBits.indexState == null) currentBits.indexState = IndexState.INITIAL;
						if(currentBits.indexState != IndexState.INITIAL)
							continue outer;
					}
					System.out.format("processing block %d %d\n", currentBits.blockNo, currentBits.readCountBits);

					try (Bzip2BlockInputStream stream = new Bzip2BlockInputStream(xmlDumpFile, currentBits.readCountBits)) {
						final BlockEntry be = currentBits;
						Indexer indexerJob = new Indexer(stream, be.readCountBits);
						indexerJob.addEventListener(indexerEventListener);
						IndexerEventListener stopper = new IndexerEventListener() {
							@Override
							public void onPageTagEnd(IndexerEvent event, long currentTagEndPos) {}
							@Override
							public void onPageStart(IndexerEvent event) {}
							@Override
							public void onNewTitle(IndexerEvent event, String title, long pageTagStartPos) {}
							@Override
							public void onEndOfStream(IndexerEvent event, boolean filePos) {
								blockController.setBlockFinished(be.blockNo);
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
				}
			}
	}

	@Override
	public void close() throws IOException {
		threadPool.shutdown();
	}
}

