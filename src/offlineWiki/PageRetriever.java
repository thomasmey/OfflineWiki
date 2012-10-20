/*
 * Copyright (C) 2012 Thomas Meyer
 */

package offlineWiki;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

class PageRetriever {

	private final InputStream in;
	private final Map<Integer,QName> levelNameMap;

	private WikiPage.Builder currentPageBuilder;
	private int level;
	private XMLStreamReader xsr;

	public PageRetriever(InputStream in) throws IOException, XMLStreamException {
		this.in = in;
		this.levelNameMap = new HashMap<Integer,QName>();

		XMLInputFactory xif = XMLInputFactory.newFactory();
		xif.setProperty("javax.xml.stream.isCoalescing", true);
		xsr = xif.createXMLStreamReader(in);
	}

	public WikiPage getNext() throws XMLStreamException, NumberFormatException, IOException {
		return parsePage();
	}

	private WikiPage parsePage() throws NumberFormatException, XMLStreamException, IOException {

		while(xsr.hasNext()) {
			xsr.next();

			int eventType = xsr.getEventType();
			switch(eventType) {

			case XMLStreamConstants.START_ELEMENT:
				// add current level to map
				level++;
				QName currentName = xsr.getName();
				levelNameMap.put(level, currentName);

				// process "WikiPage"
				if(currentName.getLocalPart().equals("page")) {
					if(currentPageBuilder == null) {
						currentPageBuilder = new WikiPage.Builder();
					} else {
						return currentPageBuilder.build();
					}
				}
				break;

			case XMLStreamConstants.END_ELEMENT:
				levelNameMap.remove(level);
				level--;
				if(level==0) {
					xsr.close();
					in.close();
					return currentPageBuilder.build();
				}
				break;

			case XMLStreamConstants.CHARACTERS:
				String currentText = xsr.getText();
				QName levelPage = null;

				if(level >= 2) {
					levelPage = levelNameMap.get(2);
					if(levelPage.getLocalPart().equals("page")) {
						QName levelSubPage;
						if(level == 3) {
							levelSubPage = levelNameMap.get(level);
							if(levelSubPage.getLocalPart().equals("title")) {
								currentPageBuilder.setTitle(currentText);
							}
							if(levelSubPage.getLocalPart().equals("id")) {
								currentPageBuilder.setId(Long.parseLong(currentText));
							}
						} else if (level == 4) {
							levelSubPage = levelNameMap.get(level);
							if(levelSubPage.getLocalPart().equals("id")) {
								currentPageBuilder.setRevisionId(Long.parseLong(currentText));
							}
							if(levelSubPage.getLocalPart().equals("comment")) {
								currentPageBuilder.setComment(currentText);
							}
							if(levelSubPage.getLocalPart().equals("text")) {
								currentPageBuilder.setText(currentText);
							}
						}
					}
				}
				break;
			}
		}

		return null;
	}
}
