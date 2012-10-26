package offlineWiki.fileindex;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Comparator;

public class FileIndexUtility implements FileIndexConstants {

	public void sort() {
		
	}

	public <T extends FileIndexAccessable<T>> long binarySearch(File indexFile, String indexName, T key,  Comparator<? super T> c) {

		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(indexFile + "." + indexName + filePostfix, "r");
			long fileSize = raf.length();

			if(key.isEntrySizeConstant()) {
				int entrySize = key.getEntrySize();

				int low = 0;
				int high = (int) (fileSize / entrySize);

				while(low <= high) {
					int mid = (low + high) / 2;
					raf.seek(mid * entrySize);
					T entry = key.read(raf);
					int comp = c.compare(entry, key);
					if(comp < 0) {
						low = mid + 1;
					} else if (comp > 0) {
						high = mid - 1;
					} else if (comp == 0){
						return mid;
					}
				}
				return -(low + 1);
			} else {
				long low = 0;
				long high = fileSize;

				while(low <= high) {
					long mid = (low + high) / 2;
					mid = findEntryNext(raf, mid);
					T entry = key.read(raf);
					int comp = c.compare(entry, key);
					if(comp < 0) {
						low = findEntryNext(raf, mid);
					} else if (comp > 0) {
						high = findEntryBefore(raf, mid);
					} else if (comp == 0){
						return mid;
					}
				}
				return -(findEntryNext(raf, low));
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if(raf != null)
				try {
					raf.close();
				} catch (IOException e) {}
		}

		return 0;
	}

	private static long findEntryNext(RandomAccessFile raf, long pos) throws IOException {
		// seek to next entry (marker)
		raf.seek(pos);
		
		return pos;
	}

	private static long findEntryBefore(RandomAccessFile raf, long pos) throws IOException {
		// seek to next entry (marker)
		raf.seek(pos);
		return pos;
	}
}
