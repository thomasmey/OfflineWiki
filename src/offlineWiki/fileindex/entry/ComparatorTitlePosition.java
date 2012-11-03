package offlineWiki.fileindex.entry;

import java.util.Comparator;

public class ComparatorTitlePosition implements Comparator<TitlePosition> {

	@Override
	public int compare(TitlePosition o1, TitlePosition o2) {
		String s1 = o1.getTitle();
		String s2 = o2.getTitle();
		return s1.compareTo(s2);
	}
}