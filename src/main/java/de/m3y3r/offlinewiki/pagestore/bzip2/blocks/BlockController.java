package de.m3y3r.offlinewiki.pagestore.bzip2.blocks;

import java.util.Iterator;

import de.m3y3r.offlinewiki.pagestore.bzip2.BlockEntry;

public interface BlockController {

	Iterator<BlockEntry> getBlockIterator();

	BlockEntry getLatestEntry();

	void setBlockFinished(long blockNo);
}
