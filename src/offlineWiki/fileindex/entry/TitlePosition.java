package offlineWiki.fileindex.entry;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import offlineWiki.fileindex.FileIndexAccessable;

public class TitlePosition implements FileIndexAccessable<TitlePosition> {

	private final char[] title;
	private final long position;
	private final long blockNo;

	@Override
	public TitlePosition read(DataInput in) {
		// TODO Auto-generated method stub
		return null;
	}

	public TitlePosition(String title, long position, long blockNo) {
		super();
		this.title = title.toCharArray();
		this.position = position;
		this.blockNo = blockNo;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeShort(title.length);
		for(char c : title)
			out.writeChar(c);
		out.writeLong(position);
		out.writeLong(blockNo);
	}

	@Override
	public boolean isEntrySizeConstant() {
		return false;
	}

	@Override
	public int getEntrySize() {
		throw new UnsupportedOperationException();
	}

}