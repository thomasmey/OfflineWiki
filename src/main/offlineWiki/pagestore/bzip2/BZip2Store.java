/*
 * Copyright 2012 Thomas Meyer
 */

package offlineWiki.pagestore.bzip2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
	private BZip2RandomInputStream bzin;

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
		try {
			if(bzin != null)
				bzin.close();
		} catch(IOException ex) {}
	}

	@Override
	public boolean exists() {
		try {
			File baseFile = OfflineWiki.getInstance().getXmlDumpFile();
			blockPositionIndex = new IndexReader<BlockPosition>(baseFile, "blockPos", comparatorBlockPosition);
			titlePositionIndex = new IndexReader<TitlePosition>(baseFile, "titlePos", comparatorTitlePosition);
			bzin = new BZip2RandomInputStream(baseFile);
			return true;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {}
		return false;
	}

	@Override
	public List<String> getTitleAscending(String title, int noMaxHits) {
		List<String> resultSet = new ArrayList<String>();

		long pos;
		List<TitlePosition> listTitlePos;
		synchronized (titlePositionIndex) {
			// get titles and offsets in uncompressed stream
			listTitlePos = new ArrayList<TitlePosition>();
			pos = titlePositionIndex.binarySearch(new TitlePosition(title, 0));

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
				resultSet.add(e.getTitle());

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

			return resultSet;
		}
	}

	@Override
	public void convert() {
		Indexer indexer = new Indexer();
		indexer.run();

		// open index files after index creation
		exists();
	}

	@Override
	public WikiPage retrieveByTitel(String title) {

		long pos;
		TitlePosition e = null;
		synchronized (titlePositionIndex) {
			// get title and offset in uncompressed stream
			pos = titlePositionIndex.binarySearch(new TitlePosition(title, 0));

			try {
				if(pos >= 0) {
					// exact match
					e = titlePositionIndex.getObjectAt(pos);
				} else {
					return null;
				}
			} catch (ClassNotFoundException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

		BlockPosition b1 = null;
		synchronized (blockPositionIndex) {
			pos = blockPositionIndex.binarySearch(new BlockPosition(0, e.getPosition()));
			try {
				b1 = blockPositionIndex.getPreviousObjectAt(-pos);
			} catch (ClassNotFoundException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

		synchronized (bzin) {
			//skip in the compressed bzip2 file to the given block
			PageRetriever pr = null;
			WikiPage wp = null;
			try {
				bzin.skipToBlockAt(b1.getBlockPosition());
				// skip in the uncompressed output to the correct position
				bzin.skip(e.getPosition() - b1.getUncompressedPosition());
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
			}
			return wp;
		}
	}

}
