package offlineWiki.fileindex;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class FileIndexWriter<E extends FileIndexAccessable<E>> implements FileIndexConstants {

	private DataOutputStream out;
	private final Checksum checksum = new CRC32();

	public FileIndexWriter(File inputFile, String indexName) throws FileNotFoundException {
		out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(inputFile + "." + indexName + filePostfix)));
	}

	public boolean add(E e) {

		boolean rc = false;
		try {
			if(e.isEntrySizeConstant())
				e.write(out);
			else {
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				DataOutputStream dout = new DataOutputStream(bout);
				e.write(dout);
				byte[] data = bout.toByteArray();

				checksum.reset();
				checksum.update(data, 0, data.length);

				out.write(marker);
				out.write(data);
				out.writeLong(checksum.getValue());
			}
			rc = true;

		} catch (IOException ex) {}
		return rc;
	}

	public void close() throws IOException {
		out.close();
		out = null;
	}
}
