package de.m3y3r.offlinewiki.pagestore.bzip2;

import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
public class LuceneIndexerEventHandler implements IndexerEventListener, Flushable {

	private final IndexWriter index;
	private final Logger logger;
	private final List<Document> documents;

	public LuceneIndexerEventHandler(File indexDir) throws IOException {
		this.logger = Logger.getLogger(Config.LOGGER_NAME);

		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
		Directory directory = FSDirectory.open(indexDir.toPath());
		IndexWriter indexWriter = new IndexWriter(directory, iwc);
		this.index = indexWriter;
		this.documents = new ArrayList<>();
	}

	@Override
	public void onPageStart(IndexerEvent event) {}

	@Override
	public void onPageTagEnd(IndexerEvent event, long currentTagEndPos) {}

	@Override
	public void onEndOfStream(IndexerEvent event, boolean normalEnd) {}

	@Override
	public void onNewTitle(IndexerEvent event, String title, long pageTagStartPos) {
		try {
			addToIndex((Indexer) event.getSource(), index, title, pageTagStartPos);
		} catch(IOException e) {
			logger.log(Level.SEVERE, "Adding title to index failed!", e);
		}
	}

	private void addToIndex(Indexer indexer, IndexWriter index, String pageTitel, long currentTagUncompressedPosition) throws IOException {

		String title = pageTitel;
		long blockPositionInBits = indexer.getBlockStartPosition();

		Document d = new Document();
		d.add(new StringField("title", title, Field.Store.YES));
		d.add(new SortedDocValuesField("title", new BytesRef(title)));

		d.add(new LongPoint("pageUncompressedPosition", currentTagUncompressedPosition));
		d.add(new StoredField("pageUncompressedPosition", currentTagUncompressedPosition));
		d.add(new LongPoint("blockPositionInBits", blockPositionInBits));
		d.add(new StoredField("blockPositionInBits", blockPositionInBits));

		synchronized (this) {
			documents.add(d);
		}
	}

	@Override
	public void onIndexingFinished(IndexerEvent event) {
		try {
			flush();
			index.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void flush() throws IOException {
		synchronized (this) {
			index.addDocuments(documents);
		}
		index.commit();
		documents.clear();
	}
}

