package de.m3y3r.offlinewiki.pagestore.bzip2;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import de.m3y3r.offlinewiki.Config;

/**
 * stores indexer result in lucene
 * @author thomas
 *
 */
public class LuceneIndexerEventHandler implements IndexerEventListener {

	private IndexWriter index;
	private int maxTitleLen;
	private int titleCount = 0;
	private final Logger logger;

	public LuceneIndexerEventHandler(File indexDir) throws IOException {
		this.logger = Logger.getLogger(Config.LOGGER_NAME);
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
		Directory directory = FSDirectory.open(indexDir.toPath());
		this.index = new IndexWriter(directory, iwc);
	}

	@Override
	public void onPageStart(IndexerEvent event) {
	}

	@Override
	public void onEndOfStream(IndexerEvent event, boolean normalEnd) {
		try {
			index.commit();
			index.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onNewTitle(IndexerEvent event, String title, long pageTagStartPos) {
		try {
			addToIndex((Indexer) event.getSource(), index, title, pageTagStartPos);
			titleCount++;
			if(titleCount % 1000 == 0) {
				logger.log(Level.FINE,"Processed {0} pages", titleCount);
				index.commit();
			}
		} catch(IOException e) {
			logger.log(Level.SEVERE, "Adding title to index failed!", e);
		}
	}

	@Override
	public void onPageTagEnd(IndexerEvent event, long currentTagEndPos) {
	}

	private void addToIndex(Indexer indexer, IndexWriter index, String pageTitel, long currentTagUncompressedPosition) throws IOException {

		String title = pageTitel;
		if(title.length() > maxTitleLen) {
			maxTitleLen = title.length();
			logger.log(Level.INFO, "Longest title \"{0}\" with size {1}", new Object[] {title, maxTitleLen});
		}

		long blockPositionInBits = indexer.getBlockStartPosition();

		Document d = new Document();
		d.add(new StringField("title", title, Field.Store.YES));
		d.add(new SortedDocValuesField("title", new BytesRef(title)));

		d.add(new LongPoint("pageUncompressedPosition", currentTagUncompressedPosition));
		d.add(new StoredField("pageUncompressedPosition", currentTagUncompressedPosition));
//		d.add(new LongPoint("blockUncompressedPosition", blockUncompressedPosition));
//		d.add(new StoredField("blockUncompressedPosition", blockUncompressedPosition));
		d.add(new LongPoint("blockPositionInBits", blockPositionInBits));
		d.add(new StoredField("blockPositionInBits", blockPositionInBits));

		index.addDocument(d);
	}
}

