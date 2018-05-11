package de.m3y3r.offlinewiki.frontend;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.logging.Logger;

import de.m3y3r.offlinewiki.pagestore.Store;
import de.m3y3r.offlinewiki.OfflineWiki;
import de.m3y3r.offlinewiki.WikiPage;

public class StdInOutDriver implements Runnable {
	
	private final Store<WikiPage, String> pageStore;
	private final Logger log;

	public StdInOutDriver() {
		this.pageStore = OfflineWiki.getInstance().getPageStore();
		this.log = Logger.getLogger(StdInOutDriver.class.getName());
	}

	@Override
	public void run() {

		String searchArticle = null;
		List<String> wpSet = null;

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

		// get first word
		try {
			searchArticle = in.readLine();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		while (searchArticle != null) {

			// input starting with # lists the index.. (there seems to be no wikipage for '###' in the de dump!)
			if(searchArticle.startsWith("###")) {
				wpSet = pageStore.getIndexKeyAscending(20, searchArticle.substring(3,searchArticle.length()));

				// search >= key
				for(String page : wpSet) {
					System.out.printf("Next key: %s\n", page);
				}
			} else {

				WikiPage wp = pageStore.retrieveByIndexKey(searchArticle);

				if(wp == null) {
					System.out.printf("%s", "No matches found!\n");
				} else {
					System.out.println(wp.getText());
				}
			}

			// get next word
			try {
				searchArticle = in.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
