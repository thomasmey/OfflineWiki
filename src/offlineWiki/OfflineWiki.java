/*
 * Copyright (C) 2012 Thomas Meyer
 */

package offlineWiki;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

class OfflineWiki implements Runnable {

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
			System.err.println("arg[0]=filename of uncompressed xml dump missing!");
			System.exit(4);
		}

		new OfflineWiki(args[0]).run();
	}

	private OfflineWiki(String fileName) {
		instance = this;
		log = Logger.getAnonymousLogger();
		log.setLevel(Level.ALL);
//		log.setLevel(Level.INFO);
		threadPool = Executors.newCachedThreadPool();

		xmlDumpFile = new File(fileName);
		pageStore = new SerialisedFsStore();

		if (!pageStore.exists()) {
//			new Converter().convert();
			new Converter().convertToIndex();
		}

		interactionDriver = new ConsoleDriver();
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
