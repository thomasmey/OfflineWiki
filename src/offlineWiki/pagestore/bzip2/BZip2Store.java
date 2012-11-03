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

import common.io.objectstream.index.IndexReader;

import offlineWiki.fileindex.entry.BlockPosition;
import offlineWiki.fileindex.entry.ComparatorBlockPosition;
import offlineWiki.fileindex.entry.ComparatorTitlePosition;
import offlineWiki.fileindex.entry.TitlePosition;
import offlineWiki.pagestore.PageStore;

import offlineWiki.OfflineWiki;
import offlineWiki.WikiPage;

public class BZip2Store implements PageStore<WikiPage> {

	private IndexReader<TitlePosition> titlePositionIndex;
	private IndexReader<BlockPosition> blockPositionIndex;
	private final Comparator<TitlePosition> comparatorTitlePosition = new ComparatorTitlePosition();
	private final Comparator<BlockPosition> comparatorBlockPosition = new ComparatorBlockPosition();

	public BZip2Store() {
	}

	@Override
	public void commit() {
		// TODO Auto-generated method stub
	}

	@Override
	public void store(WikiPage wp) {
		// TODO Auto-generated method stub
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean exists() {
		try {
			File baseFile = OfflineWiki.getInstance().getXmlDumpFile();
			blockPositionIndex = new IndexReader<BlockPosition>(baseFile, "blockPos", comparatorBlockPosition);
			titlePositionIndex = new IndexReader<TitlePosition>(baseFile, "titlePos", comparatorTitlePosition);
			return true;
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
//				e = titlePositionIndex.getObjectAt(-pos);
			} else {
				// exact match
				e = titlePositionIndex.getObjectAt(pos);
			}
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		for(int i = 0; i < noMaxHits && e != null; i++) {
			listTitlePos.add(e);
			try {
				e = titlePositionIndex.getNextObject();
			} catch (ClassNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				e = null;
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				e = null;
			}
		}

		// get blockNo and compressed offset from uncompressed offsets
		for(TitlePosition tp: listTitlePos) {
			pos = blockPositionIndex.binarySearch(new BlockPosition(0, 0, tp.getPosition()));
			BlockPosition b1 = null;
			BlockPosition b2 = null;
			try {
				b1 = blockPositionIndex.getPreviousObjectAt(-pos);
				b2 = blockPositionIndex.getNextObject();
			} catch (ClassNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			System.out.println("titel= " + tp.getTitle() + " search pos: " + tp.getPosition() + " found b1= " + b1.getUncompressedPosition() + " b2= " + b2.getUncompressedPosition());
		}

		return resultSet;
	}

	@Override
	public void convert() {
		Indexer indexer = new Indexer();
		indexer.run();
	}

}
