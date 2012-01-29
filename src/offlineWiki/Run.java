/*
 * Copyright (C) 2012 Thomas Meyer
 */

package offlineWiki;

import java.io.Console;
import java.io.IOException;
import java.util.TreeMap;

import javax.xml.stream.XMLStreamException;

public class Run {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws XMLStreamException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException, XMLStreamException {

		// get index
		Indexer ix = new Indexer(args[0]);
		TreeMap<String, Long> articleIndexTitle = ix.getArticleIndexTitle();

		PageRetriever pr = new PageRetriever(args[0]);

		Console c = System.console();
		String searchArticle = null;
		String key = null;

		// get first word
		System.out.print("> ");
		searchArticle = c.readLine();

		while (searchArticle != null) {

			Long offset = null;
			
			// input starting with # lists the index.. (there seems to be no wikipage for '#' in the de dump!)
			if(searchArticle.startsWith("#")) {
				key = articleIndexTitle.ceilingKey(searchArticle.substring(1,searchArticle.length()));
				// search >= key
				for(int i=0;i< 20 && key != null;i++) {
					key = articleIndexTitle.higherKey(key);
					System.out.println("Next key: " + key);
				}
			} else
				offset = articleIndexTitle.get(searchArticle);
				
			if(offset == null) {

				key = articleIndexTitle.ceilingKey(searchArticle);
				// search >= key
				for(int i=0;i< 20 && key != null;i++) {
					key = articleIndexTitle.higherKey(key);
					System.out.println("Next key: " + key);
				}
			} else {
				System.err.println(pr.get(offset).getText());
			}
			
			// get next word
			System.out.print("> ");
			searchArticle = c.readLine();
		}

	}

}
