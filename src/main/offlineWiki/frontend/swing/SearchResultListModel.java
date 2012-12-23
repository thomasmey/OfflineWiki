package offlineWiki.frontend.swing;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import offlineWiki.OfflineWiki;
import offlineWiki.WikiPage;
import offlineWiki.pagestore.PageStore;

public class SearchResultListModel implements ListModel<String> {

	private final PageStore<WikiPage> pageStore;
	private final Logger log;
	private final List<ListDataListener> listDataListeners;
	private final int searchCount = 30;

	private List<String> titles;

	public SearchResultListModel() {
		this.pageStore = OfflineWiki.getInstance().getPageStore();
		this.log = OfflineWiki.getInstance().getLogger();
		this.listDataListeners = new CopyOnWriteArrayList<ListDataListener>();
	}

	@Override
	public int getSize() {
		if(titles == null)
			return 0;

		return titles.size();
	}

	@Override
	public String getElementAt(int index) {
		if(index < 0 || index > titles.size())
			throw new IllegalArgumentException();

		return titles.get(index);
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

		if(searchText != null) {
			if(!searchText.equals("")) {
				titles = pageStore.getTitleAscending(searchText, searchCount);
			} else {
				titles = Collections.emptyList();
			}

			ListDataEvent listDataEvent = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, titles.size());
			for(ListDataListener l: listDataListeners) {
				l.contentsChanged(listDataEvent);
			}
		}
	}
}
