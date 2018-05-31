package de.m3y3r.offlinewiki.utility;

import java.util.EventObject;

public interface DownloadEventListener {

	void onDownloadStart(EventObject event);
	void onDownloadFinished(EventObject event);

	void onProgress(EventObject event, long currentFileSize);

	void onNewByte(EventObject event, int b);

}
