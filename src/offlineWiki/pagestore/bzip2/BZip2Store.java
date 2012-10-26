package offlineWiki.pagestore.bzip2;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.SortedSet;

import offlineWiki.OfflineWiki;
import offlineWiki.pagestore.PageStore;
import offlineWiki.fileindex.entry.BlockPosition;
import offlineWiki.fileindex.entry.TitlePosition;
import offlineWiki.WikiPage;
import offlineWiki.fileindex.FileIndexAccessable;
import offlineWiki.fileindex.FileIndexReader;

public class BZip2Store implements PageStore<WikiPage> {

	private FileIndexReader fir;
	
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
			File inFile = OfflineWiki.getInstance().getXmlDumpFile();
			
			fir = new FileIndexReader<TitlePosition>(inFile, "titlePos");
			FileIndexReader<BlockPosition> blockPos = new FileIndexReader<BlockPosition>(inFile, "blockPos");
			return true;
		} catch (FileNotFoundException e) {}
		return false;
	}

	@Override
	public SortedSet<WikiPage> getTitleAscending(String title, int noMaxHits) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void convert() {
		new Indexer().createIndex();
	}

}
