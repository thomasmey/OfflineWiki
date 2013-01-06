package offlineWiki.pagestore;

public interface IndexKeyFilter<I> {

	/** Deutsche Wikipedia-Gedenk-Methode */
	boolean isRelevant(I indexKey);
}
