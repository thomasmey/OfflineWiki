/*
 * Copyright 2012 Thomas Meyer
 */

package de.m3y3r.offlinewiki.pagestore.bzip2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
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

import de.m3y3r.offlinewiki.OfflineWiki;
import de.m3y3r.offlinewiki.PageRetriever;
import de.m3y3r.offlinewiki.WikiPage;
import de.m3y3r.offlinewiki.pagestore.Store;
import de.m3y3r.offlinewiki.utility.BufferInputStream;
import de.m3y3r.offlinewiki.utility.Bzip2BlockInputStream;
import de.m3y3r.offlinewiki.utility.SplitFile;

public class BZip2Store implements Store<WikiPage, String> {

	private Logger logger = Logger.getLogger(BZip2Store.class.getName());

	@Override
	public List<String> getIndexKeyAscending(int noMaxHits, String indexKey) {

		File df = getIndexDir();
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

		File df = getIndexDir();
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

	private File getIndexDir() {
		SplitFile inputFile = OfflineWiki.getInstance().getXmlDumpFile();
		File df = new File(inputFile.getParentFile(), inputFile.getBaseName() + ".index");
		return df;
	}

	@Override
	public WikiPage retrieveByIndexKey(String title) {

		long blockPositionInBits = 0;
		long blockUncompressedPosition = 0;
		long pageUncompressedPosition = 0;

		// get title and offset in uncompressed stream
		SplitFile inputFile = OfflineWiki.getInstance().getXmlDumpFile();
		File df = new File(inputFile.getParentFile(), inputFile.getBaseName() + ".index");

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

		SplitFile baseFile = OfflineWiki.getInstance().getXmlDumpFile();
		try (
				Bzip2BlockInputStream bbin = new Bzip2BlockInputStream(baseFile, blockPositionInBits, Long.MAX_VALUE);
				BufferInputStream in = new BufferInputStream(bbin);
				BZip2CompressorInputStream bZip2In = new BZip2CompressorInputStream(in, false);) {
			// skip to next page; set uncompressed byte position
			// i.e. the position relative to block start
			long nextPagePos = pageUncompressedPosition - blockUncompressedPosition;
			bZip2In.skip(nextPagePos);
			PageRetriever pr = new PageRetriever(bZip2In);
			WikiPage page = pr.getNext();
			return page;
		} catch(IOException e) {
			logger.log(Level.SEVERE, "", e);
		}
		return null;
	}

	@Override
	public void close() {
	}
}
