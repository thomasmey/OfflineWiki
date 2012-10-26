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
		SortedSet<WikiPage> wpSet = null;

		// get first word
		searchArticle = console.readLine("%s", "> ");

		while (searchArticle != null) {

			// input starting with # lists the index.. (there seems to be no wikipage for '###' in the de dump!)
			if(searchArticle.startsWith("###")) {
				wpSet = pageStore.getTitleAscending(searchArticle.substring(3,searchArticle.length()), 20);

				// search >= key
				for(int i=0;i< 20 && wpSet != null;i++) {
					console.printf("Next key: %s", wpSet);
				}
			} else {
				wpSet = pageStore.getTitleAscending(searchArticle, 3);

				if(wpSet.size() == 0) {
					console.printf("%s", "No matches found!");
				} else {
					WikiPage cwp = wpSet.first();
					if(cwp.getTitle().equals(searchArticle)) {
						System.out.println(cwp);
					} else {
						
					}
				}
			}
			// get next word
			searchArticle = console.readLine("%s", "> ");
		}
	}
}