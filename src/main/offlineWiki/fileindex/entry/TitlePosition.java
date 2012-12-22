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

	@Override
	public int hashCode() {
		return title.hashCode();
	}

	@Override
	public String toString() {
		return '[' + title + '-' + position + ']';
	}

	public long getPosition() {
		return position;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof TitlePosition) {
			TitlePosition pos = (TitlePosition) obj;
			return title.equals(pos.title) && position == pos.position;
		}
		return false;
	}

	public TitlePosition(String title, long position) {

		if(position < 0 || title == null)
			throw new IllegalArgumentException(title + '-' + position);

		this.title = title;
		this.position = position;
	}

	public String getTitle() {
		return title;
	}
};