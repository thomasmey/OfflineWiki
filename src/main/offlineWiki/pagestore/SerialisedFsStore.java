/*
 * Copyright 2012 Thomas Meyer
 */

package offlineWiki.pagestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;

import offlineWiki.OfflineWiki;
import offlineWiki.WikiPage;

class SerialisedFsStore implements PageStore<WikiPage> {

	private BlockingQueue<WikiPage> blockingQueue;
	private Future<?> writer;

	private class Writer implements Runnable {

		private boolean running = true;

		@Override
		public void run() {

			while(running) {
				try {
					WikiPage wp = SerialisedFsStore.this.blockingQueue.take();
					store(wp);
				} catch (InterruptedException e) {
					running = false;
				}
			}
			// remove remaining elements from queue
			List<WikiPage> list = new ArrayList<WikiPage>();
			SerialisedFsStore.this.blockingQueue.drainTo(list);
			for(WikiPage wp: list)
				store(wp);
		}
		void store(WikiPage wp) {
			ObjectOutputStream out = null;

			File outDir = null;
			String title = wp.getTitle();
			if(title.length() <= 5)
				outDir = new File("shortTitles");
			else
				outDir = new File(title.substring(0, 4));
			if(!outDir.exists())
				outDir.mkdir();

			File outputFile = new File(outDir,title);
			try {
				out = new ObjectOutputStream(new FileOutputStream(outputFile));
				out.writeObject(wp);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			} finally {
				try {
					if(out != null)
						out.close();
				} catch (IOException e) {}
			}
		}
	}

	public SerialisedFsStore() {
		blockingQueue = new ArrayBlockingQueue<WikiPage>(1000);
		writer = OfflineWiki.getInstance().getThreadPool().submit(new Writer());
	}

	@Override
	public void commit() {}

	@Override
	public void store(WikiPage wp) {
		try {
			blockingQueue.put(wp);
		} catch (InterruptedException e) {}
	}

	@Override
	public void close() {
		writer.cancel(true);
	}

	@Override
	public List<String> getTitleAscending(String title, int noMaxHits) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean exists() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void convert() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public WikiPage retrieveByTitel(String title) {
		// TODO Auto-generated method stub
		return null;
	}

}
