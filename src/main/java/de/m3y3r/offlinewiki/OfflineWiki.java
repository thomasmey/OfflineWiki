/*
 * Copyright 2012 Thomas Meyer
 */

package de.m3y3r.offlinewiki;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import de.m3y3r.offlinewiki.frontend.swing.SwingDriver;
import de.m3y3r.offlinewiki.pagestore.Store;
import de.m3y3r.offlinewiki.pagestore.bzip2.BZip2Store;
import de.m3y3r.offlinewiki.pagestore.bzip2.BlockFinder;
import de.m3y3r.offlinewiki.pagestore.bzip2.FileBasedBlockController;
import de.m3y3r.offlinewiki.pagestore.bzip2.FileBasedBlockController.BlockEntry;
import de.m3y3r.offlinewiki.pagestore.bzip2.IndexerController;
import de.m3y3r.offlinewiki.pagestore.bzip2.LuceneIndexerEventHandler;
import de.m3y3r.offlinewiki.utility.DownloadEventListener;
import de.m3y3r.offlinewiki.utility.Downloader;
import de.m3y3r.offlinewiki.utility.SplitFile;

public class OfflineWiki implements Runnable {

	private final Store<WikiPage, String> pageStore;
	private final Properties config;
	private final ExecutorService threadPool;
	private final Logger log;
	private final Runnable interactionDriver;
	private final CountDownLatch interactionDriverLatch;

	private File configFile;

	private static OfflineWiki instance;

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws XMLStreamException 
	 */
	public static void main(String[] args) {
		new OfflineWiki().run();
	}

	private OfflineWiki() {
		instance = this;
		log = Logger.getLogger(Config.LOGGER_NAME);
		threadPool = Executors.newCachedThreadPool();

		Properties configDefaults = new Properties();
		try(InputStream inDefault = this.getClass().getResourceAsStream("/config.xml")) {
			configDefaults.loadFromXML(inDefault);
		} catch (NullPointerException | IOException e) {
			e.printStackTrace();
		}
		configFile = new File("config.xml");

		config = new Properties(configDefaults);
		try(InputStream inState = new FileInputStream(configFile)) {
			config.loadFromXML(inState);
		} catch (NullPointerException | IOException e) {
			e.printStackTrace();
		}

		pageStore = new BZip2Store();

		interactionDriverLatch = new CountDownLatch(1);

		//		interactionDriver = new ConsoleDriver();
		//		interactionDriver = new StdInOutDriver();
		interactionDriver = new SwingDriver(interactionDriverLatch);
	}

	public static OfflineWiki getInstance() {
		return instance;
	}

	public Store<WikiPage, String> getPageStore() {
		return pageStore;
	}

	public SplitFile getXmlDumpFile() {
		String xmlDumpUrl = config.getProperty("xmlDumpUrl");
		if(xmlDumpUrl == null)
			return null;

		File targetDir = new File(".");
		String baseName = Downloader.getBaseNameFromUrl(xmlDumpUrl);
		SplitFile targetDumpFile = new SplitFile(targetDir, baseName);
		return targetDumpFile;
	}

	public ExecutorService getThreadPool() {
		return threadPool;
	}

	@Override
	public void run() {

		if(Boolean.parseBoolean(config.getProperty("firstStart"))) {
			String xmlDumpUrl = config.getProperty("xmlDumpUrl", config.getProperty("defaultXmlDumpUrl"));
			config.setProperty("xmlDumpUrl", xmlDumpUrl);
			config.setProperty("firstStart", "false");
			commitConfig();
		}

		if(!Boolean.parseBoolean(config.getProperty("downloadFinished"))) {
			String xmlDumpUrl = config.getProperty("xmlDumpUrl");
			File targetDir = new File(".");
			String baseName = Downloader.getBaseNameFromUrl(xmlDumpUrl);
			SplitFile targetDumpFile = new SplitFile(targetDir, baseName);

			//FIXME: isRestart must not be true when etag of wiki dump file did change
			boolean isRestart = true;

			BlockEntry restartPos = null;
			File blockFile = new File(targetDir, baseName + ".blocks");
			if(!isRestart)
				blockFile.delete();
			else {
				restartPos = FileBasedBlockController.getLastEntry(blockFile);
			}

			File indexDir = new File(targetDir, baseName + ".index");
			if(!isRestart) {
				try {
					Files.walk(indexDir.toPath())
					.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(File::delete);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			indexDir.mkdir();
			config.setProperty("indexDir", indexDir.getPath());
			commitConfig();

			try {
				//TODO: Buffer size is aligned to be SD card friendly, is 4MB okay?
				final int bufferSize = (int) Math.pow(2, 22);
				Downloader downloader = new Downloader(xmlDumpUrl, targetDumpFile, restartPos != null ? restartPos.readCountBits / 8 : null, bufferSize);
				Map<String, List<String>> headers = downloader.doHead();
				long targetFileSize = Downloader.getFileSizeFromHeaders(headers);

				FileBasedBlockController blockController = new FileBasedBlockController(blockFile);
				BlockFinder blockFinder = new BlockFinder(restartPos != null ? restartPos : null);
				blockFinder.addEventListener(blockController);

				// This event handler is called concurrently, be careful with synchronization
				LuceneIndexerEventHandler indexEventHandler = new LuceneIndexerEventHandler(indexDir);
				IndexerController indexController = new IndexerController(targetDumpFile, indexEventHandler, blockController);
				DownloadEventListener del = new DownloadEventListener() {
					@Override
					public void onProgress(EventObject event, long currentFileSize) {
						try {
							blockController.flush();
							indexEventHandler.flush();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					@Override
					public void onDownloadStart(EventObject event) {}
					@Override
					public void onDownloadFinished(EventObject event) {
						try {
							blockController.close();
							config.setProperty("blockSearchFinished", "true");
						} catch (IOException e) {
							e.printStackTrace();
						} finally {
							config.setProperty("downloadFinished", "true");
							commitConfig();
						}
					}
					@Override
					public void onNewByte(EventObject event, int b) {}
				};
				downloader.addEventListener(blockFinder);
				downloader.addEventListener(del);

				threadPool.submit(downloader);
				threadPool.submit(indexController);
			} catch(IOException e) {
				e.printStackTrace();
			}
		}

		threadPool.execute(interactionDriver);

		// wait for interaction driver to finish
		try(Store<WikiPage,String> ps = this.pageStore) {
			interactionDriverLatch.await();
		} catch (InterruptedException | IOException e) {
			log.log(Level.SEVERE, "wait for interaction driver to finish was interruppted!", e);
		} finally {
			// shutdown pool
			threadPool.shutdown();
			threadPool.shutdownNow();
		}
	}

	private synchronized void commitConfig() {
		try(OutputStream outState = new FileOutputStream(configFile)) {
			config.storeToXML(outState, null);
		} catch (NullPointerException | IOException e) {
			e.printStackTrace();
		}
	}

	public Properties getConfig() {
		return config;
	}

}
