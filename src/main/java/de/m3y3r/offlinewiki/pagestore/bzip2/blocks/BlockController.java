package de.m3y3r.offlinewiki.pagestore.bzip2.blocks;

import java.util.Iterator;

public interface BlockController {

	Iterator<BlockEntry> getBlockIterator();

	BlockEntry getLatestEntry();

	void setBlockFinished(long blockNo);
}
