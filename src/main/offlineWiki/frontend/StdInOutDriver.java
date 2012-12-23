package offlineWiki.frontend;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.logging.Logger;

import offlineWiki.OfflineWiki;
import offlineWiki.WikiPage;
import offlineWiki.pagestore.PageStore;

public class StdInOutDriver implements Runnable {
	
	private final PageStore<WikiPage> pageStore;
	private final Logger log;

	public StdInOutDriver() {
		this.pageStore = OfflineWiki.getInstance().getPageStore();
		this.log = OfflineWiki.getInstance().getLogger();
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
				wpSet = pageStore.getTitleAscending(searchArticle.substring(3,searchArticle.length()), 20);

				// search >= key
				for(String page : wpSet) {
					System.out.printf("Next key: %s\n", page);
				}
			} else {

				WikiPage wp = pageStore.retrieveByTitel(searchArticle);

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
