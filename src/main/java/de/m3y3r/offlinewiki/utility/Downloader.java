package de.m3y3r.offlinewiki.utility;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.m3y3r.offlinewiki.Config;
import de.m3y3r.offlinewiki.pagestore.bzip2.BlockFinder;

public class Downloader implements Runnable {

	private final URL url;
	private final SplitFile dumpFile;
	private final boolean isRestart;
	private final List<DownloadEventListener> eventListeners;
	private final int bufferSize;

	// observer
	private final BlockFinder blockFinder;

	public Downloader(String xmlDumpUrl, SplitFile targetDumpFile, boolean isRestart, int bufferSize, BlockFinder blockFinder) throws MalformedURLException {
		this.url = new URL(xmlDumpUrl);
		this.dumpFile = targetDumpFile;
		this.isRestart = isRestart;
		this.blockFinder = blockFinder;
		this.bufferSize = bufferSize;
		this.eventListeners = new CopyOnWriteArrayList<>();
	}

	public void addEventListener(DownloadEventListener el) {
		eventListeners.add(el);
	}

	/**
	 * get HTTP headers only
	 * @return
	 */
	public Map<String, List<String>> doHead() {
		try {
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("HEAD");
			con.connect();

			// process headers
			Map<String, List<String>> headers = con.getHeaderFields();
//			etag = headers.get("ETag").get(0);
			return headers;
		} catch (IOException e) {
			Logger.getLogger(Config.LOGGER_NAME).log(Level.SEVERE, "HEAD failed!", e);
		}
		return null;
	}

	public static long getFileSizeFromHeaders(Map<String, List<String>> headers) {
		List<String> lens = headers.get("Content-Length");
		// get remote size
		return Long.parseLong(lens.get(0));
	}

	@Override
	public void run() {
		fireEventDownloadStart();

//		if(dumpFile.length() == totalFileSize) {
//			fireEventDownloadFinished();
//			return;
//		}

		try(SplitFileOutputStream outputStream = new SplitFileOutputStream(dumpFile, Config.SPLIT_SIZE)) {
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			if(isRestart) { // restart existing download
				long lenCommited = dumpFile.length();
				con.addRequestProperty("Range", "bytes=" + lenCommited + "-");
				outputStream.seek(lenCommited);

				//feed the last 6 bytes into the block finder
				if(blockFinder != null) {
					try(SplitFileInputStream inRes = new SplitFileInputStream(dumpFile, Config.SPLIT_SIZE)) {
						long lenRead = lenCommited - 6;
						if(lenRead <= 0)
							lenRead = 0;
						inRes.seek(lenRead);
						for(; lenRead == lenCommited; lenRead++) {
							int b = inRes.read();
							if(b >= 0)
								blockFinder.update(b);
							else
								throw new IllegalStateException();
						}
					}
				}
			}
			con.connect();
			try (InputStream in = con.getInputStream()) {
				download(in, outputStream);
			}
			fireEventDownloadFinished();
		} catch (IOException e) {
			Logger.getLogger(Config.LOGGER_NAME).log(Level.SEVERE, "GET failed!", e);
		}
	}

	void download(InputStream in, OutputStream out) throws IOException {
		long c = 0;

		try (
				BufferedInputStream bin = new BufferedInputStream(in, bufferSize);
				BufferedOutputStream bout = new BufferedOutputStream(out, bufferSize)
			) {
			int b = bin.read();

			while(b >= 0) {
				if(Thread.interrupted())
					return;

				if(blockFinder != null)
					blockFinder.update(b);
				bout.write(b);
				b = bin.read();

				c++;
				if(c % bufferSize == 0) {
					//FIXME:
					bout.flush();
					fireEventProgress(c);
				}
			}
		}
	}

	private void fireEventProgress(long currentFileSize) {
		for(DownloadEventListener e: eventListeners) {
			EventObject event = new EventObject(this);
			e.onProgress(event,currentFileSize);
		}
	}

	private void fireEventDownloadFinished() {
		for(DownloadEventListener e: eventListeners) {
			EventObject event = new EventObject(this);
			e.onDownloadFinished(event);
		}
	}

	private void fireEventDownloadStart() {
		for(DownloadEventListener e: eventListeners) {
			EventObject event = new EventObject(this);
			e.onDownloadStart(event);
		}
	}

	public static String getBaseNameFromUrl(String xmlDumpUrl) {
		int i = xmlDumpUrl.lastIndexOf('/');
		if(i < 0)
			return null;

		return xmlDumpUrl.substring(i, xmlDumpUrl.length());
	}
}
