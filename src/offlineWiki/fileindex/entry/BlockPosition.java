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

	private final long blockNo;
	private final long blockPosition;
	private final long uncompressedPosition;

	public BlockPosition(long currBlockNo, long currBlockPosition, long currStreamPosition) {
		blockNo = currBlockNo;
		blockPosition = currBlockPosition;
		uncompressedPosition = currStreamPosition;
	}

	@Override
	public String toString() {
		return "" + '[' + blockNo + '-' + blockPosition + '-' + uncompressedPosition + ']'; 
	}

	public long getBlockNo() {
		return blockNo;
	}

	public long getBlockPosition() {
		return blockPosition;
	}

	public long getUncompressedPosition() {
		return uncompressedPosition;
	}
}