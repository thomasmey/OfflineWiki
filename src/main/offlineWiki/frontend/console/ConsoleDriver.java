/*
 * Copyright 2012 Thomas Meyer
 */

package offlineWiki.frontend.console;

import java.io.Console;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import offlineWiki.OfflineWiki;
import offlineWiki.WikiPage;
import offlineWiki.pagestore.PageStore;

public class ConsoleDriver implements Runnable {

	private final PageStore<WikiPage> pageStore;
	private final Logger log;

	public ConsoleDriver() {
		this.pageStore = OfflineWiki.getInstance().getPageStore();
		this.log = OfflineWiki.getInstance().getLogger();
	}

	@Override
	public void run() {
		
		Console console = System.console();
		if(console==null) {
			log.log(Level.SEVERE,"Failed to grab console!\n");
			return;
		}

		String searchArticle = null;
		SortedSet<String> wpSet = null;

		// get first word
		searchArticle = console.readLine("%s", "> ");

		while (searchArticle != null) {

			// input starting with # lists the index.. (there seems to be no wikipage for '###' in the de dump!)
			if(searchArticle.startsWith("###")) {
				wpSet = pageStore.getTitleAscending(searchArticle.substring(3,searchArticle.length()), 20);

				// search >= key
				for(String page : wpSet) {
					console.printf("Next key: %s\n", page);
				}
			} else {

				WikiPage wp = pageStore.retrieveByTitel(searchArticle);

				if(wp == null) {
					console.printf("%s", "No matches found!\n");
				} else {
					System.out.println(wp.getText());
				}
			}
			// get next word
			searchArticle = console.readLine("%s", "> ");
		}
	}
}