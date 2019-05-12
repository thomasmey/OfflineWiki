/*
 * Copyright 2012 Thomas Meyer
 */
package de.m3y3r.offlinewiki.pagestore.bzip2;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import de.m3y3r.offlinewiki.OfflineWiki;
import de.m3y3r.offlinewiki.PageRetriever;
import de.m3y3r.offlinewiki.WikiPage;
import de.m3y3r.offlinewiki.pagestore.Store;
import de.m3y3r.offlinewiki.pagestore.bzip2.blocks.BlockController;
import de.m3y3r.offlinewiki.pagestore.bzip2.blocks.BlockEntry;
import de.m3y3r.offlinewiki.pagestore.bzip2.blocks.jdbc.JdbcBlockController;
import de.m3y3r.offlinewiki.pagestore.bzip2.index.IndexAccess;
import de.m3y3r.offlinewiki.pagestore.bzip2.index.jdbc.JdbcIndexAccess;
import de.m3y3r.offlinewiki.utility.BufferInputStream;
import de.m3y3r.offlinewiki.utility.Bzip2BlockInputStream;
import de.m3y3r.offlinewiki.utility.SplitFile;

public class BZip2Store implements Store<WikiPage, String> {

	private Logger logger = Logger.getLogger(BZip2Store.class.getName());

	private final IndexAccess index;
	private final BlockController blockController;

	public BZip2Store() {
		this.index = new JdbcIndexAccess();
		this.blockController = new JdbcBlockController();
	}

	@Override
	public List<String> getIndexKeyAscending(int noMaxHits, String indexKey) {
		List<String> resultSet = index.getKeyAscending(noMaxHits, indexKey);
		return resultSet;
	}

	@Override
	public List<String> getIndexKeyAscendingLike(int maxReturnCount, String likeKey) {
		return index.getKeyAscendingLike(maxReturnCount, likeKey);
	}

	@Override
	public WikiPage retrieveByIndexKey(String title) {

		long blockPositionInBits;
		long pageUncompressedPosition;

		long[] positions = index.getKey(title);

		blockPositionInBits = positions[0];
		pageUncompressedPosition = positions[1];

		SplitFile baseFile = OfflineWiki.getInstance().getXmlDumpFile();
		Iterator<BlockEntry> blockIterator = blockController.getBlockIterator(blockPositionInBits);
		long[] blockPositions = new long[] {blockIterator.next().readCountBits, blockIterator.next().readCountBits};
		try (
				Bzip2BlockInputStream bbin = new Bzip2BlockInputStream(baseFile, blockPositions);
				BufferInputStream in = new BufferInputStream(bbin);
				BZip2CompressorInputStream bZip2In = new BZip2CompressorInputStream(in, false);) {
			// skip to next page; set uncompressed byte position
			// i.e. the position relative to block start
			long nextPagePos = pageUncompressedPosition;
			bZip2In.skip(nextPagePos);
			try(PageRetriever pr = new PageRetriever(bZip2In)) {
				WikiPage page = pr.getNext();
				return page;
			}
		} catch(IOException e) {
			logger.log(Level.SEVERE, "", e);
		}
		return null;
	}

	@Override
	public void close() {
	}
}
