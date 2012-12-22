/*
 * Copyright 2012 Thomas Meyer
 */

package offlineWiki.fileindex.entry;

import java.io.Serializable;

public class TitlePosition implements Serializable {

	/**
	 * object format v1
	 */
	private static final long serialVersionUID = 1L;

	private final String title;
	private final long position;
	private final long blockNo;

	@Override
	public int hashCode() {
		return title.hashCode();
	}

	@Override
	public String toString() {
		return '[' + title + '-' + position + '-' + blockNo + ']';
	}

	public long getPosition() {
		return position;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof TitlePosition) {
			TitlePosition pos = (TitlePosition) obj;
			return title.equals(pos.title) && position == pos.position && blockNo == pos.blockNo;
		}
		return false;
	}

	public TitlePosition(String title, long position, long blockNo) {
		super();
		this.title = title;
		this.position = position;
		this.blockNo = blockNo;
	}

	public String getTitle() {
		return title;
	}
};