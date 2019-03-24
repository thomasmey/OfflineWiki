package de.m3y3r.offlinewiki.pagestore.bzip2.index.jdbc;

import de.m3y3r.offlinewiki.pagestore.bzip2.index.IndexerEvent;
import de.m3y3r.offlinewiki.pagestore.bzip2.index.IndexerEventListener;

public class JdbcIndexerEventHandler implements IndexerEventListener {

	@Override
	public void onPageStart(IndexerEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPageTagEnd(IndexerEvent event, long currentTagEndPos) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onNewTitle(IndexerEvent event, String title, long pageTagStartPos) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onEndOfStream(IndexerEvent event, boolean filePos) {
		// TODO Auto-generated method stub

	}

}
