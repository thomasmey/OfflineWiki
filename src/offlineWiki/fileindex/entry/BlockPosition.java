package offlineWiki.fileindex.entry;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import offlineWiki.fileindex.FileIndexAccessable;

public class BlockPosition implements FileIndexAccessable<BlockPosition>{

	private final long blockNo;
	private final long blockPosition;
	private final long uncompressedPosition;

	public BlockPosition(long currBlockNo, long currBlockPosition, long currStreamPosition) {
		blockNo = currBlockNo;
		blockPosition = currBlockPosition;
		uncompressedPosition = currStreamPosition;
	}

	@Override
	public BlockPosition read(DataInput in) throws IOException {
		long blockNo = in.readLong();
		long blockPosition = in.readLong();
		long uncompressedPosition = in.readLong();
		return new BlockPosition(blockNo, blockPosition, uncompressedPosition);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeLong(blockNo);
		out.writeLong(blockPosition);
		out.writeLong(uncompressedPosition);
	}

	@Override
	public boolean isEntrySizeConstant() {
		return true;
	}

	@Override
	public int getEntrySize() {
		return Long.SIZE / 8 * 3;
	}

}
