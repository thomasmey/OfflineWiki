package offlineWiki.fileindex.entry;

import java.util.Comparator;

public class ComparatorBlockPosition implements Comparator<BlockPosition> {
	@Override
	public int compare(BlockPosition o1, BlockPosition o2) {
		Long o1l = o1.getUncompressedPosition();
		Long o2l = o2.getUncompressedPosition();
		return o1l.compareTo(o2l);
	}
}