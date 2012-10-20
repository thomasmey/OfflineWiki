/*
 * Copyright (C) 2012 Thomas Meyer
 */

package offlineWiki;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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

	// we need to do the XML parsing ourself to get a connection between the current element file offset
	// and the parser state...
	public void convertToIndex() {

		if(inputFile.getName().endsWith(".bz2")) {
			throw new IllegalArgumentException("Only uncompressed xml dump is supported!");
		}

		DataOutputStream out = null;
		int level = 0;
		Map<Integer,StringBuilder> levelNameMap = new HashMap<Integer,StringBuilder>();

		StringBuilder sbElement = null;
		int[] sbChar = new int[1024*1024*16];
		int sbCharPos = 0;
		long currentTagPos = 0;
		int currentMode = 0;
		int prevMode = 0;

		int currentChar = 0;
		Utf8Reader utf8Reader = null;
		int titleCount = 0;

		try {
			utf8Reader = new Utf8Reader(inputFile);
			out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(inputFile + ".index")));

			// read first
			currentChar = utf8Reader.read();

			while(currentChar >= 0) {

				prevMode = currentMode;
				switch (currentMode) {
				case 0: // characters

					if(currentChar == '<') {
						sbElement = new StringBuilder(32);
						currentMode = 1;
						break;
					}
					sbChar[sbCharPos] = currentChar;
					sbCharPos++;
					if(sbCharPos > sbChar.length) {
						log.log(Level.SEVERE,"Error! Buffer full!");
						sbCharPos=0;
					}
					break;

				case 1: // element name open
					if(currentChar == '/') {
						currentMode = 2;
						break;
					}
					if(currentChar == ' ') {
						currentMode = 3;
						break;
					}
					if(currentChar == '>') {
						level++;
						levelNameMap.put(level, sbElement);
						sbCharPos = 0;
						currentMode = 0;
						break;
					}
					sbElement.appendCodePoint(currentChar);
					break;
				case 2: // element name close
					if(currentChar == '>') {
						levelNameMap.remove(level);
						level--;
						sbCharPos = 0;
						currentMode = 0;
						break;
					}
					sbElement.appendCodePoint(currentChar);
					break;
				case 3: // element attributes
					if(currentChar == '"') {
						currentMode = 5;
						break;
					}

					if(currentChar == '/') {
						currentMode = 4;
						break;
					}

					if(currentChar == '>') {
						level++;
						levelNameMap.put(level, sbElement);
						sbCharPos = 0;
						currentMode = 0;
						break;
					}
					break;
				case 4: // single element
					if(currentChar == '>') {
						sbCharPos = 0;
						currentMode = 		0;
						break;
					}

				case 5: // attribute assignment
					if(currentChar == '"') {
						currentMode = 3;
						break;
					}

				}

				if(prevMode==1) {
					if(currentMode==0 && level == 2 && levelNameMap.get(2).toString().equals("page")) {
						// save position
						currentTagPos = utf8Reader.getCurrenFilePos() - 6;
					}
					if(currentMode==2 && level == 3 && levelNameMap.get(2).toString().equals("page") && levelNameMap.get(3).toString().equals("title")) {
						StringBuilder sb = new StringBuilder(256);
						for(int i=0; i< sbCharPos; i++) {
							sb.appendCodePoint(sbChar[i]);
						}
						String title = sb.toString();
						Map.Entry<String,Long> indexEntry = new AbstractMap.SimpleImmutableEntry<String, Long>(title, currentTagPos);
						IndexEntryUtility.writeObject(out, indexEntry);
						titleCount++;
						if(titleCount % 500 == 0) {
							out.flush();
							log.log(Level.FINE,"Processed {0} pages", titleCount);
						}
					}
				}

				// read next
				currentChar = utf8Reader.read();
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} finally {
			// close resources
			try {
				if(utf8Reader != null)
					utf8Reader.close();
			} catch (IOException e) {}
			try {
				if(out != null)
					out.close();
			} catch (IOException e) {}
		}
	}
}

class IndexEntryUtility /*implements Serializable*/ {
	private final static int titleLenMax = 255;
	
	static void writeObject(DataOutputStream out, Map.Entry<String,Long> entry) throws IOException {
		String key = entry.getKey();
		long value = entry.getValue();

		short keyLen;
		char[] keyArray;
		long position;

		if(key.length() > titleLenMax) {
			OfflineWiki.getInstance().getLogger().log(Level.SEVERE, "Titel too long: \"{0}\" - {1}", new String[] {key, Integer.valueOf(key.length()).toString()});
			throw new IllegalArgumentException("Titel is too long!");
		}

		keyLen = (short) key.length();
		keyArray = Arrays.copyOf(key.toCharArray(), titleLenMax);
		position = value;
		out.writeShort(keyLen);
		for(char c : keyArray)
			out.writeChar(c);
		out.writeLong(position);
	}
}
