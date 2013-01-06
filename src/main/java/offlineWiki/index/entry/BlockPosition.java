package offlineWiki.index.entry;

public class BlockPosition {
	public final long blockPositionInBits;
	public final long uncompressedPosition;

	public BlockPosition(java.lang.Long blockPositionInBits, java.lang.Long uncompressedPosition) {
		this.blockPositionInBits = blockPositionInBits;
		this.uncompressedPosition = uncompressedPosition;
	}

	public long getBlockPositionInBits() {
		return blockPositionInBits;
	}

	public long getUncompressedPosition() {
		return uncompressedPosition;
	}

}
