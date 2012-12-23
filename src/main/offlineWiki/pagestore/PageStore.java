/*
 * Copyright 2012 Thomas Meyer
 */

package offlineWiki.pagestore;

import java.util.List;

public interface PageStore<T> {

	void commit();
	void store(T wp);
	void close();
	boolean exists();

	/** list must be sorted ascending */
	List<String> getTitleAscending(String title, int noMaxHits);
	T retrieveByTitel(String title);
	void convert();
}
