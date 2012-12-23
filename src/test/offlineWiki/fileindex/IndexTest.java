/*
 * Copyright 2012 Thomas Meyer
 */

package offlineWiki.fileindex;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import common.io.objectstream.index.IndexReader;

import offlineWiki.fileindex.entry.BlockPosition;
import offlineWiki.fileindex.entry.TitlePosition;

public class IndexTest {

	private static final String dataFileName = "/home/thomas/dewiki-latest-pages-articles.xml.bz2";
	private static final String titlePosIndexName = "titlePos";

	@Test
	public void binarySearch() {

		Comparator<TitlePosition> titleComparator = new Comparator<TitlePosition>() {

			@Override
			public int compare(TitlePosition o1, TitlePosition o2) {
				String s1 = new String(o1.getTitle());
				String s2 = new String(o2.getTitle());
				return s1.compareTo(s2);
			}
		};

		File dataFile = new File(dataFileName);

		long currentTime = System.currentTimeMillis();
		//		BlockPosition blockPos = IndexReader.binarySearch(indexFile, "blockPos", new BlockPosition(3, 0, 0), blockNoComparator);
		long afterTime = System.currentTimeMillis();
		//		System.out.println("Search for blockNo = 3 resulted in pos= " + blockPos + " - time was " + (afterTime - currentTime) + "ms");

		currentTime = System.currentTimeMillis();
		//		TitlePosition titlePos = IndexReader.binarySearch(indexFile, "titlePos", new TitlePosition("Test", 0, 0), titleComparator);
		afterTime = System.currentTimeMillis();
		//		System.out.println("Search for title \"Test\" resulted in pos= " + titlePos + " - time was " + (afterTime - currentTime) + "ms");
	}

	@Test
	public void readIndexPartRaw() throws ClassNotFoundException, IOException {
		ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(new File(dataFileName + '.' + titlePosIndexName + ".index.0"))));

		int objCount = 0;
		try {
			TitlePosition titlePosition = (TitlePosition) in.readObject();
			while(titlePosition != null) {
				objCount++;
				titlePosition = (TitlePosition) in.readObject();
			}
		} catch(EOFException e) {}
		finally {
			in.close();
		}

		System.out.println("Object count= " + objCount);
	}

	@Test
	public void readCompleteIndexRaw() throws ClassNotFoundException, IOException {
		ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(new File(dataFileName + '.' + titlePosIndexName + ".index"))));

		int objCount = 0;
		try {
			TitlePosition titlePosition = (TitlePosition) in.readObject();
			while(titlePosition != null) {
				objCount++;
				titlePosition = (TitlePosition) in.readObject();
			}
		} catch(EOFException e) {}
		finally {
			in.close();
		}

		System.out.println("Object count= " + objCount);
	}

	@Test
	public void readCompleteIndex() throws ClassNotFoundException, IOException {
		IndexReader<TitlePosition> titelIndexReader = new IndexReader<TitlePosition>(new File(dataFileName), titlePosIndexName, null);

		int objCount = 0;

		try {
			TitlePosition titlePosition = titelIndexReader.getNextObject();
			while(titlePosition != null) {
				objCount++;
				titlePosition = titelIndexReader.getNextObject();
			}
		} catch(EOFException e) {
		} finally {
			titelIndexReader.close();
		}

		System.out.println("Object count= " + objCount);

	}

	@Test
	public void htmlEntitiesInIndex() throws ClassNotFoundException, IOException {
		IndexReader<TitlePosition> titelIndexReader = new IndexReader<TitlePosition>(new File(dataFileName), titlePosIndexName, null);

		Set<String> htmlEntities = new HashSet<String>();
		try {
			TitlePosition titlePosition = titelIndexReader.getNextObject();
			while(titlePosition != null) {
				String title = titlePosition.getTitle();
				int iFrom = title.indexOf('&');
				int iTo = title.indexOf(';');
				if(iFrom >= 0 && iTo >= 0 && iTo > iFrom) { 
					String entity = title.substring(iFrom, iTo + 1);
					htmlEntities.add(entity);
				}

				titlePosition = titelIndexReader.getNextObject();
			}
		} catch(EOFException e) {
		} finally {
			titelIndexReader.close();
		}

		for(String entity : htmlEntities) {
			System.out.println("entity found= " + entity);
		}

		Assert.assertEquals(0, htmlEntities.size());
	}
}
