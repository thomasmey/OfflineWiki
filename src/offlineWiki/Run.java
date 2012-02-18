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

		if(args.length  < 1) {
			System.out.println("arg[0]=filename of uncompressed xml dump missing!");
			System.exit(4);
		}
		
		// get index
		Indexer ix = new Indexer(args[0]);
		TreeMap<String, Long> articleIndexTitle = ix.getArticleIndexTitle();

		PageRetriever pr = new PageRetriever(args[0]);

		Thread t1 = new ConsoleDriver(articleIndexTitle, pr);
		t1.start();

//		Thread t2 = new SwingDriver(articleIndexTitle, pr);
//		t2.start();

	}

}
