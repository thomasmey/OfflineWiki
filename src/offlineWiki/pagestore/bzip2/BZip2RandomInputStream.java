package offlineWiki.pagestore.bzip2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

public class BZip2RandomInputStream extends InputStream {

	private final RandomAccessFile randomAccess;
	private final BZip2CompressorInputStream inExtern;

	private final InputStream inIntern = new InputStream() {
		@Override
		public int read() throws IOException {
			return randomAccess.read();
		}
	};

	public BZip2RandomInputStream(File baseFile, long blockPos) throws IOException {
		randomAccess = new RandomAccessFile(baseFile, "r");
		inExtern = new BZip2CompressorInputStream(inIntern);
		skipToBlockAt(blockPos);
	}

	public void close() throws IOException {
	}

	public void skipToBlockAt(long blockPosition) throws IOException {
		long pos = blockPosition / 8;
		byte remain = (byte) (blockPosition % 8);
		randomAccess.seek(pos);
		inExtern.resetBlock(remain);
	}

	@Override
	public int read() throws IOException {
		return inExtern.read();
	}

}
