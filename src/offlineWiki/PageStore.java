package offlineWiki;

import java.util.SortedSet;

interface PageStore<T> {

	void commit();
	void store(T wp);
	void close();
	boolean exists();
	SortedSet<T> getTitleAscending(String title, int noMaxHits);
}
