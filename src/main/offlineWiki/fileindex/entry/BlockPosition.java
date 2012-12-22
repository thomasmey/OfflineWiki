/*
 * Copyright 2012 Thomas Meyer
 */

package offlineWiki.fileindex.entry;

import java.io.Serializable;

public class BlockPosition implements Serializable {

	/**
	 * object format v1
	 */
	private static final long serialVersionUID = 1L;

	private final long blockPositionInBits;
	private final long uncompressedPosition;

	public BlockPosition(long currBlockPositionInBits, long currStreamPosition) {
		blockPositionInBits = currBlockPositionInBits;
		uncompressedPosition = currStreamPosition;
	}

	@Override
	public String toString() {
		return "" + '[' + blockPositionInBits + '-' + uncompressedPosition + ']'; 
	}

	public long getBlockPosition() {
		return blockPositionInBits;
	}

	public long getUncompressedPosition() {
		return uncompressedPosition;
	}
}