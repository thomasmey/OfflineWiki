package offlineWiki.pagestore.bzip2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

public class BZip2RandomInputStream extends InputStream {

	private final RandomAccessFile randomAccess;
	private final BZip2CompressorInputStream inExtern;
	private final ByteBuffer byteBuffer;
	private byte[] byteArray;

	private final InputStream inIntern = new InputStream() {
		@Override
		public int read() throws IOException {
			return randomAccess.read();
		}
	};

	public BZip2RandomInputStream(File baseFile, long blockPos) throws IOException {
		randomAccess = new RandomAccessFile(baseFile, "r");
		inExtern = new BZip2CompressorInputStream(inIntern);
		if(blockPos >= 0)
			skipToBlockAt(blockPos);

		byteArray = new byte[1024 * 1024];
		byteBuffer = ByteBuffer.wrap(byteArray);
	}

	public BZip2RandomInputStream(File baseFile) throws IOException {
		this(baseFile,-1);
	}

	public void close() throws IOException {
	}

	/** skip to the given bzip2 block
	 * 
	 * @param blockPosition block position in bits. must be a valid block start position
	 * @throws IOException
	 */
	public void skipToBlockAt(long blockPosition) throws IOException {
		long pos = blockPosition / 8;
		byte remain = (byte) (blockPosition % 8);
		randomAccess.seek(pos);

		//skip remaining  bits
		inExtern.resetBlock(remain);
		byteBuffer.rewind();
		byteBuffer.limit(0);
	}

	@Override
	public int read() throws IOException {
		if(byteBuffer.remaining() == 0) {
			int len = inExtern.read(byteArray);
			if(len < 0)
				return len;

			byteBuffer.rewind();
			byteBuffer.limit(len);
		}
		return byteBuffer.get();
	}

}
