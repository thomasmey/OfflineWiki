/*
 * Copyright 2012 Thomas Meyer
 */

package offlineWiki.pagestore.bzip2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.stream.XMLStreamException;

import common.io.objectstream.index.IndexReader;

import offlineWiki.fileindex.entry.BlockPosition;
import offlineWiki.fileindex.entry.ComparatorBlockPosition;
import offlineWiki.fileindex.entry.ComparatorTitlePosition;
import offlineWiki.fileindex.entry.TitlePosition;
import offlineWiki.pagestore.PageStore;

import offlineWiki.OfflineWiki;
import offlineWiki.PageRetriever;
import offlineWiki.WikiPage;

public class BZip2Store implements PageStore<WikiPage> {

	private final Comparator<TitlePosition> comparatorTitlePosition;
	private final Comparator<BlockPosition> comparatorBlockPosition;

	private IndexReader<TitlePosition> titlePositionIndex;
	private IndexReader<BlockPosition> blockPositionIndex;

	public BZip2Store() {
		comparatorTitlePosition = new ComparatorTitlePosition();
		comparatorBlockPosition = new ComparatorBlockPosition();
	}

	@Override
	public void commit() { /* nop */ }

	@Override
	public void store(WikiPage wp) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void close() {
	}

	@Override
	public boolean exists() {
		try {
			File baseFile = OfflineWiki.getInstance().getXmlDumpFile();
			blockPositionIndex = new IndexReader<BlockPosition>(baseFile, "blockPos", comparatorBlockPosition);
			titlePositionIndex = new IndexReader<TitlePosition>(baseFile, "titlePos", comparatorTitlePosition);
			return true;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {}
		return false;
	}

	@Override
	public SortedSet<WikiPage> getTitleAscending(String title, int noMaxHits) {
		SortedSet<WikiPage> resultSet = new TreeSet<WikiPage>();

		// get titles and offsets in uncompressed stream
		List<TitlePosition> listTitlePos = new ArrayList<TitlePosition>();
		long pos = titlePositionIndex.binarySearch(new TitlePosition(title, 0, 0));
		TitlePosition e = null;

		try {
			if(pos < 0) {
				// unexact match
				e = titlePositionIndex.getObjectAt(-pos);
			} else {
				// exact match
				e = titlePositionIndex.getObjectAt(pos);
			}
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		for(int i = 0; i < noMaxHits && e != null; i++) {
			listTitlePos.add(e);
			try {
				e = titlePositionIndex.getNextObject();
			} catch (ClassNotFoundException e1) {
				e1.printStackTrace();
				e = null;
			} catch (IOException e1) {
				e1.printStackTrace();
				e = null;
			}
		}

		// get blockNo and compressed offset from uncompressed offsets
		for(TitlePosition tp: listTitlePos) {
			pos = blockPositionIndex.binarySearch(new BlockPosition(0, 0, tp.getPosition()));
			BlockPosition b1 = null;
			try {
				b1 = blockPositionIndex.getPreviousObjectAt(-pos);
			} catch (ClassNotFoundException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			//open compressed bzip2 file and skip to the given block
			File baseFile = OfflineWiki.getInstance().getXmlDumpFile();
			PageRetriever pr = null;
			BZip2RandomInputStream bzin = null;
			WikiPage wp = null;
			try {
				bzin = new BZip2RandomInputStream(baseFile, b1.getBlockPosition());
				// skip in the uncompressed output to the correct position
				bzin.skip(tp.getPosition() - b1.getUncompressedPosition());
				pr = new PageRetriever(bzin);
				wp = pr.getNext();
			} catch (IOException ex) {
				ex.printStackTrace();
			} catch (XMLStreamException ex) {
				ex.printStackTrace();
			} finally {
				try {
					if(pr != null)
						pr.close();
				} catch(IOException ex) {}
				try {
					if(pr != null)
						bzin.close();
				} catch(IOException ex) {}
			}
			if(wp != null) {
				resultSet.add(wp);
			}
		}

		return resultSet;
	}

	@Override
	public void convert() {
		Indexer indexer = new Indexer();
		indexer.run();
	}

}
