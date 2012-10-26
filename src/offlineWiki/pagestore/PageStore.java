package offlineWiki.pagestore;

import java.util.SortedSet;

public interface PageStore<T> {

	void commit();
	void store(T wp);
	void close();
	boolean exists();
	SortedSet<T> getTitleAscending(String title, int noMaxHits);
	void convert();
}
