/*
 * Copyright 2012 Thomas Meyer
 */

package offlineWiki;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import offlineWiki.frontend.console.ConsoleDriver;
import offlineWiki.frontend.swing.SwingDriver;
import offlineWiki.pagestore.PageStore;
import offlineWiki.pagestore.bzip2.BZip2Store;

public class OfflineWiki implements Runnable {

	private final PageStore<WikiPage> pageStore;
	private final File xmlDumpFile;
	private final ExecutorService threadPool;
	private final Logger log;
	private final Runnable interactionDriver;
	private static OfflineWiki instance;

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws XMLStreamException 
	 */
	public static void main(String[] args) {

		if(args.length < 1) {
			System.err.println("arg[0] = filename of uncompressed xml dump missing!");
			System.exit(4);
		}

		new OfflineWiki(args[0]).run();
	}

	private OfflineWiki(String fileName) {
		instance = this;
		log = Logger.getLogger("mainLog");
		threadPool = Executors.newCachedThreadPool();

		xmlDumpFile = new File(fileName);
		pageStore = new BZip2Store();

		if (!pageStore.exists()) {
			log.log(Level.INFO, "Creating index files. Sadly this takes very long, please be patient!");
			pageStore.convert();
		}

		interactionDriver = new ConsoleDriver();
//		interactionDriver = new SwingDriver();
	}

	public static OfflineWiki getInstance() {
		return instance;
	}

	public PageStore<WikiPage> getPageStore() {
		return pageStore;
	}

	public File getXmlDumpFile() {
		return xmlDumpFile;
	}

	public ExecutorService getThreadPool() {
		return threadPool;
	}

	public Logger getLogger() {
		return log;
	}

	@Override
	public void run() {
		threadPool.execute(interactionDriver);
		threadPool.shutdown();
		threadPool.shutdownNow();
	}
}
