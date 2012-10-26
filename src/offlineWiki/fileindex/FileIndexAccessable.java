package offlineWiki.fileindex;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public interface FileIndexAccessable<E> {
	E read(DataInput in) throws IOException;
	void write(DataOutput out) throws IOException;
	boolean isEntrySizeConstant();
	int getEntrySize();
}