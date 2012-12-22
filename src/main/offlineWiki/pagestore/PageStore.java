/*
 * Copyright 2012 Thomas Meyer
 */

package offlineWiki.pagestore;

import java.util.SortedSet;

public interface PageStore<T> {

	void commit();
	void store(T wp);
	void close();
	boolean exists();
	SortedSet<String> getTitleAscending(String title, int noMaxHits);
	T retrieveByTitel(String title);
	void convert();
}
