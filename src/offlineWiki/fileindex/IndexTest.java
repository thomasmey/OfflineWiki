/*
 * Copyright 2012 Thomas Meyer
 */

package offlineWiki.fileindex;

import java.io.File;
import java.util.Comparator;

import common.io.objectstream.index.IndexReader;

import offlineWiki.fileindex.entry.BlockPosition;
import offlineWiki.fileindex.entry.TitlePosition;

public class IndexTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File indexFile = new File("/home/thomas/dewiki-latest-pages-articles.xml.bz2");
		Comparator<BlockPosition> blockNoComparator = new Comparator<BlockPosition>() {

			@Override
			public int compare(BlockPosition o1, BlockPosition o2) {
				return (int) (o1.getBlockNo() - o2.getBlockNo());
			}
		};

		Comparator<TitlePosition> titleComparator = new Comparator<TitlePosition>() {

			@Override
			public int compare(TitlePosition o1, TitlePosition o2) {
				String s1 = new String(o1.getTitle());
				String s2 = new String(o2.getTitle());
				return s1.compareTo(s2);
			}
		};

		long currentTime = System.currentTimeMillis();
//		BlockPosition blockPos = IndexReader.binarySearch(indexFile, "blockPos", new BlockPosition(3, 0, 0), blockNoComparator);
		long afterTime = System.currentTimeMillis();
//		System.out.println("Search for blockNo = 3 resulted in pos= " + blockPos + " - time was " + (afterTime - currentTime) + "ms");

		currentTime = System.currentTimeMillis();
//		TitlePosition titlePos = IndexReader.binarySearch(indexFile, "titlePos", new TitlePosition("Test", 0, 0), titleComparator);
		afterTime = System.currentTimeMillis();
//		System.out.println("Search for title \"Test\" resulted in pos= " + titlePos + " - time was " + (afterTime - currentTime) + "ms");
	}

}
