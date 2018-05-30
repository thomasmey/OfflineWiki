package de.m3y3r.offlinewiki.utility;

import java.util.EventObject;

public interface DownloadEventListener {

	void onProgress(EventObject event, long currentFileSize);

	void onDownloadFinished(EventObject event);

	void onDownloadStart(EventObject event);

	void onNewByte(EventObject event, int b);

}
