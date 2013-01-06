/*
 * Copyright 2012 Thomas Meyer
 */

package offlineWiki.frontend.console;

import java.io.Console;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import offlineWiki.OfflineWiki;
import offlineWiki.WikiPage;
import offlineWiki.pagestore.Store;

public class ConsoleDriver implements Runnable {

	private final Store<WikiPage, String> pageStore;
	private final Logger log;

	public ConsoleDriver() {
		this.pageStore = OfflineWiki.getInstance().getPageStore();
		this.log = Logger.getLogger(ConsoleDriver.class.getName());
	}

	@Override
	public void run() {

		Console console = System.console();
		if(console == null) {
			log.log(Level.SEVERE,"Failed to grab console!\n");
			return;
		}

		String searchArticle = null;
		List<String> wpSet = null;

		// get first word
		searchArticle = console.readLine("%s", "> ");

		while (searchArticle != null) {

			// input starting with # lists the index.. (there seems to be no wikipage for '###' in the de dump!)
			if(searchArticle.startsWith("###")) {
				wpSet = pageStore.getIndexKeyAscending(20, searchArticle.substring(3,searchArticle.length()));

				// search >= key
				for(String page : wpSet) {
					console.printf("Next key: %s\n", page);
				}
			} else {

				WikiPage wp = pageStore.retrieveByIndexKey(searchArticle);

				if(wp == null) {
					console.printf("%s", "No matches found!\n");
				} else {
					console.printf("%s", wp.getText());
				}
			}

			// get next word
			searchArticle = console.readLine("%s", "> ");
		}
	}
}