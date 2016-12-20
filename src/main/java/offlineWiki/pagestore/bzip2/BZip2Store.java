/*
 * Copyright 2012 Thomas Meyer
 */

package offlineWiki.pagestore.bzip2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import offlineWiki.pagestore.Store;

import offlineWiki.OfflineWiki;
import offlineWiki.PageRetriever;
import offlineWiki.WikiPage;

public class BZip2Store implements Store<WikiPage, String> {

	private BZip2RandomInputStream bzin;
	private Logger logger = Logger.getLogger(BZip2Store.class.getName());

	@Override
	public boolean exists() {
		boolean rc = false;
		File baseFile = OfflineWiki.getInstance().getXmlDumpFile();
		if(baseFile.exists() &&
				baseFile.isFile() &&
				baseFile.getParentFile().listFiles(
						(d, fn) -> {if(fn.startsWith(baseFile.getName()) && fn.endsWith(".index")) return true; else return false;})
				.length > 0) {
			rc = true;
		}

		return rc;
	}

	@Override
	public List<String> getIndexKeyAscending(int noMaxHits, String indexKey) {

		File inputFile = OfflineWiki.getInstance().getXmlDumpFile();
		File df = new File(inputFile.getParentFile(), inputFile.getName() + ".index");

		try(IndexReader reader = DirectoryReader.open(FSDirectory.open(df.toPath()));) {
			IndexSearcher searcher = new IndexSearcher(reader);

			Sort sorter = new Sort();
			sorter.setSort(new SortField("title", Type.STRING));
			TermRangeQuery query = TermRangeQuery.newStringRange("title", indexKey, null, true, false);
			TopDocs docs = searcher.search(query, noMaxHits, sorter);

			List<String> resultSet = new ArrayList<>();
			for(int i=0, n=docs.scoreDocs.length; i < n; i++) {
				Document document = reader.document(docs.scoreDocs[i].doc);
				resultSet.add(document.getField("title").stringValue());
			}
			return resultSet;
		} catch(IOException e) {
			logger.log(Level.SEVERE, "", e);
		}
		return null;
	}

	@Override
	public List<String> getIndexKeyAscendingLike(int maxReturnCount, String likeKey) {

		File inputFile = OfflineWiki.getInstance().getXmlDumpFile();
		File df = new File(inputFile.getParentFile(), inputFile.getName() + ".index");

		try(IndexReader reader = DirectoryReader.open(FSDirectory.open(df.toPath()));) {
			IndexSearcher searcher = new IndexSearcher(reader);

			Sort sorter = new Sort();
			sorter.setSort(new SortField("title", Type.STRING));

			//FIMXE: maybe use a FuzzyQuery here?!
			Query query = new RegexpQuery(new Term("title", likeKey));
			TopDocs docs = searcher.search(query, maxReturnCount, sorter);

			List<String> resultSet = new ArrayList<>();
			for(int i=0, n=docs.scoreDocs.length; i < n; i++) {
				Document document = reader.document(docs.scoreDocs[i].doc);
				resultSet.add(document.getField("title").stringValue());
			}
			return resultSet;
		} catch(IOException e) {
			logger.log(Level.SEVERE, "", e);
		}
		return null;
	}

	@Override
	public void convert() {
		Indexer indexer = new Indexer();
		indexer.run();
	}

	@Override
	public WikiPage retrieveByIndexKey(String title) {

		long blockPositionInBits = 0;
		long blockUncompressedPosition = 0;
		long pageUncompressedPosition = 0;

		// get title and offset in uncompressed stream
		File inputFile = OfflineWiki.getInstance().getXmlDumpFile();
		File df = new File(inputFile.getParentFile(), inputFile.getName() + ".index");

		try(IndexReader reader = DirectoryReader.open(FSDirectory.open(df.toPath()));) {
			IndexSearcher searcher = new IndexSearcher(reader);

			Query query = new TermQuery(new Term("title", title));
			TopDocs docs = searcher.search(query, 1);
			Document document = reader.document(docs.scoreDocs[0].doc);
			{
				IndexableField field = document.getField("blockPositionInBits");
				blockPositionInBits = field.numericValue().longValue();
			}

			{
				IndexableField field = document.getField("pageUncompressedPosition");
				pageUncompressedPosition = field.numericValue().longValue();
			}

			{
				IndexableField field = document.getField("blockUncompressedPosition");
				blockUncompressedPosition = field.numericValue().longValue();
			}

		} catch(IOException e) {
			logger.log(Level.SEVERE, "", e);
		}

		try {
			synchronized (bzin) {
				//skip in the compressed bzip2 file to the given block
				bzin.skipToBlockAt(blockPositionInBits);
				// skip in the uncompressed output to the correct position
				bzin.skip(pageUncompressedPosition - blockUncompressedPosition);

				try (PageRetriever pr = new PageRetriever(bzin)) {
					return pr.getNext();
				} catch (XMLStreamException | IOException ex) {
					logger.log(Level.SEVERE, "", ex);
				}
			}
		} catch(IOException e) {
			logger.log(Level.SEVERE, "", e);
		}
		return null;
	}

	@Override
	public void open() {
		File baseFile = OfflineWiki.getInstance().getXmlDumpFile();
		try {
			bzin = new BZip2RandomInputStream(baseFile);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "", e);
		}
	}

	@Override
	public void close() {
		try {
			if(bzin != null)
				bzin.close();
		} catch(IOException e) {
			logger.log(Level.SEVERE, "Failed!", e);
		}
	}
}
