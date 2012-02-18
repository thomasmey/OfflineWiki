package offlineWiki;

import java.io.Console;
import java.io.IOException;
import java.util.TreeMap;

import javax.xml.stream.XMLStreamException;

public class ConsoleDriver extends Thread {

	private TreeMap<String,Long> articleIndexTitle;
	private PageRetriever pageRetriever;

	public ConsoleDriver(TreeMap<String, Long> articleIndexTitle,
			PageRetriever pr) {
		this.pageRetriever = pr;
		this.articleIndexTitle = articleIndexTitle;
	}

	@Override
	public void run() {
		
		Console c = System.console();
		if(c==null) {
			System.out.println("Failed to grab console!\n");
			System.exit(4);
		}

		String searchArticle = null;
		String key = null;

		// get first word
		System.out.print("> ");
		searchArticle = c.readLine();

		while (searchArticle != null) {

			Long offset = null;
			
			// input starting with # lists the index.. (there seems to be no wikipage for '###' in the de dump!)
			if(searchArticle.startsWith("###")) {
				key = articleIndexTitle.ceilingKey(searchArticle.substring(3,searchArticle.length()));
				// search >= key
				for(int i=0;i< 20 && key != null;i++) {
					System.out.println("Next key: " + key);
					key = articleIndexTitle.higherKey(key);
				}
			} else {
				offset = articleIndexTitle.get(searchArticle);

				if(offset == null) {
	
					key = articleIndexTitle.ceilingKey(searchArticle);
					// search >= key
					for(int i=0;i< 20 && key != null;i++) {
						System.out.println("Next key: " + key);
						key = articleIndexTitle.higherKey(key);
					}
				} else {
//					System.out.println("offset + " + offset);
					System.err.print("*****\nPage \"" + searchArticle + "\" found\n*****\n");
					String currentWikiPage;
					try {
						currentWikiPage = pageRetriever.get(offset).getText();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return;
					} catch (XMLStreamException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return;
					}
					System.err.println(currentWikiPage);
				}
			}
			// get next word
			System.out.print("> ");
			searchArticle = c.readLine();
		}
	}

	
}
