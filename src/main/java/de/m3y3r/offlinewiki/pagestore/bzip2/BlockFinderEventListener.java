package de.m3y3r.offlinewiki.pagestore.bzip2;

import java.util.EventObject;

public interface BlockFinderEventListener {
	void onNewBlock(EventObject event, long blockNo, long readCountBits);
	void onEndOfFile(EventObject event);
}
