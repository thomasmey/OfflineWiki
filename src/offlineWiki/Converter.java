/*
 * Copyright (C) 2012 Thomas Meyer
 */

package offlineWiki;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

class Converter {

	private final PageStore<WikiPage> pageStore;
	private final File inputFile;
	private final Logger log;

	public Converter() {
		this.pageStore = OfflineWiki.getInstance().getPageStore();
		this.inputFile = OfflineWiki.getInstance().getXmlDumpFile();
		this.log = OfflineWiki.getInstance().getLogger();
	}

	public void convert() {

		int count = 0;
		log.log(Level.INFO,"Converting xml...");

		PageRetriever pr = null;
		WikiPage wp = null;
		InputStream in = null;
		try {
			in = new BufferedInputStream(new FileInputStream(inputFile));
			if(inputFile.getName().endsWith(".bz2")) {
				in = new BZip2CompressorInputStream(in);
			}

			pr = new PageRetriever(in);

			wp = pr.getNext();
			while(wp != null) {
				count++;
				pageStore.store(wp);
				if(count % 5000 == 0) {
					log.log(Level.FINE,"Conversion count = {0}", count);
					pageStore.commit();
				}

				wp = null;
				wp = pr.getNext();
			}
		} catch(IOException e) {
			e.printStackTrace();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		} finally {
			// close resources
			try {
				if(in != null)
					in.close();
			} catch (IOException e) {}
			pageStore.close();
			try {
				if(in!=null)
					in.close();
			} catch (IOException e) {}
		}
	}
}
