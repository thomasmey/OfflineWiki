/*
 * Copyright 2012 Thomas Meyer
 */

package offlineWiki.pagestore;

import java.util.List;

public interface Store<T,I> {

	/** open store */
	void open();

	/** close connection */
	void close();

	/** is the PageStore available */
	boolean exists();

	/** get index keys ascending
	 * @param maxReturnCount no of maximum hits to return
	 * @param indexKey key of index to search
	 * @return list of all matches, sorted ascending
	 */
	List<I> getIndexKeyAscending(int maxReturnCount, I indexKey);

	/** get index ascending, by a Comparable
	 * @param indexKey key of index to search
	 * @param maxReturnCount no of maximum hits to return
	 * @return list of all matches, sorted ascending
	 */
	List<I> getIndexKeyAscendingLike(int maxReturnCount, I likeKey);

	/** retrieve object by index key */
	T retrieveByIndexKey(I title);

	/** convert to PageStore format */
	void convert();
}
