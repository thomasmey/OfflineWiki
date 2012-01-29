/*
 * Copyright (C) 2012 Thomas Meyer
 */

package offlineWiki;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class PageRetriever {

	private File inFile;
	
	public PageRetriever(String filename) throws IOException {
			inFile = new File(filename);
	}

	public WikiPage get(Long offset) throws IOException, XMLStreamException {

		WikiPage currentPage = null;
		int level = 0;
		Map<Integer,QName> levelNameMap = new HashMap<Integer,QName>();

		InputStream in = new FileInputStream(inFile);
		in.skip(offset);
		in = new BufferedInputStream(in);
		XMLInputFactory xif = XMLInputFactory.newFactory();
		xif.setProperty("javax.xml.stream.isCoalescing", true);
		XMLStreamReader xsr = xif.createXMLStreamReader(in);

		while(xsr.hasNext()) {
			xsr.next();

			int eventType = xsr.getEventType();
			switch(eventType) {

			case XMLStreamReader.START_ELEMENT:
				// add current level to map
				level++;
				QName currentName = xsr.getName();
				levelNameMap.put(level, currentName);

				// process "WikiPage"
				if(currentName.getLocalPart().equals("page")) {
//					if(currentPage != null) {
//
//					}
					currentPage = new WikiPage();
				}
				break;

			case XMLStreamReader.END_ELEMENT:
				levelNameMap.remove(level);
				level--;
				if(level==0) {
					xsr.close();
					in.close();
					return currentPage;
				}
				break;

			case XMLStreamReader.CHARACTERS:
				String currentText = xsr.getText();
				
				if(level < 3)
					break;

				QName levelPage = levelNameMap.get(1);
				QName levelSubPage = levelNameMap.get(level);
				if(levelPage.getLocalPart().equals("page")) {
					if(levelSubPage.getLocalPart().equals("title")) {
						currentPage.setTitle(currentText);
					}
					if(levelSubPage.getLocalPart().equals("id")) {
						currentPage.setId(Long.parseLong(currentText));
					}
					if(levelSubPage.getLocalPart().equals("comment")) {
						currentPage.setComment(currentText);
					}
					if(levelSubPage.getLocalPart().equals("text")) {
						currentPage.setText(currentText);
					}
				}
				break;
				
			}
		}

		return null;
	}
}
