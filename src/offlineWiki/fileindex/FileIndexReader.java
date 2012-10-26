package offlineWiki.fileindex;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

public class FileIndexReader<E> implements FileIndexConstants {

	private final RandomAccessFile raf;

	public FileIndexReader(File xmlDumpFile, String indexName) throws FileNotFoundException {
		raf = new RandomAccessFile(xmlDumpFile + filePostfix, "r");
	}

}
