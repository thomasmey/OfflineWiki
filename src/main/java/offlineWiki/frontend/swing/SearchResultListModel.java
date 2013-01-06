package offlineWiki.frontend.swing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import offlineWiki.OfflineWiki;
import offlineWiki.WikiPage;
import offlineWiki.pagestore.IndexKeyFilter;
import offlineWiki.pagestore.Store;

public class SearchResultListModel implements ListModel<String> {

	private final Store<WikiPage, String> pageStore;
	private final Logger log;
	private final List<ListDataListener> listDataListeners;
	private final int searchCount = 50;

	private final List<String> titles;

	public SearchResultListModel() {
		this.pageStore = OfflineWiki.getInstance().getPageStore();
		this.log = Logger.getLogger(SearchResultListModel.class.getName());
		this.listDataListeners = new CopyOnWriteArrayList<ListDataListener>();
		this.titles = new ArrayList<String>();
	}

	@Override
	public int getSize() {
		synchronized (titles) {
			return titles.size();
		}
	}

	@Override
	public String getElementAt(int index) {
		synchronized (titles) {
			if(index < 0 || index > titles.size())
				throw new IllegalArgumentException();

			return titles.get(index);
		}
	}

	@Override
	public void addListDataListener(ListDataListener l) {
		listDataListeners.add(l);
	}

	@Override
	public void removeListDataListener(ListDataListener l) {
		listDataListeners.remove(l);
	}

	void updateSearchResultList(String searchText) {

		List<String> newTitles;

		if(searchText == null || "".equals(searchText)) {
			newTitles = Collections.emptyList();
		} else {

			/* regular expression search
			 * warning: this will do a full index scan and takes long
			 */
			if(searchText.contains("*")) {
				Pattern pattern = Pattern.compile(searchText);
				final Matcher matcher = pattern.matcher("");
				newTitles = pageStore.getIndexKeyAscending(searchCount, indexKey -> {return matcher.reset(indexKey).matches();} );
			} else {
				newTitles = pageStore.getIndexKeyAscending(searchCount, searchText);
			}
		}

		// set new titles
		synchronized (titles) {
			titles.clear();
			titles.addAll(newTitles);
			// notify all data listeners about change
			notifyDataListeners(newTitles);
		}

	}

	private void notifyDataListeners(List<String> titles) {
		ListDataEvent listDataEvent = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, titles.size());
		for(ListDataListener l: listDataListeners) {
			l.contentsChanged(listDataEvent);
		}
	}
}
