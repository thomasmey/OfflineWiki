/*
 * Copyright 2012 Thomas Meyer
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

import offlineWiki.pagestore.Store;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

class Converter {

	private final Store<WikiPage, String> pageStore;
	private final File inputFile;
	private final Logger log;

	public Converter() {
		this.pageStore = OfflineWiki.getInstance().getPageStore();
		this.inputFile = OfflineWiki.getInstance().getXmlDumpFile();
		this.log = Logger.getLogger(Converter.class.getName());
	}

	public void convert() {

		int count = 0;
		log.log(Level.INFO,"Converting xml...");

		WikiPage wp = null;

		InputStream i = null;
		try {
			i = new BufferedInputStream(new FileInputStream(inputFile));
			if(inputFile.getName().endsWith(".bz2")) {
				i = new BZip2CompressorInputStream(i);
			}
		} catch(IOException e) {
			log.log(Level.SEVERE, "failed!", e);
		}
	
		try(InputStream in = i; PageRetriever pr = new PageRetriever(i)) {

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
		} catch(IOException | XMLStreamException e) {
			log.log(Level.SEVERE, "failed!", e);
		}
	}
}
