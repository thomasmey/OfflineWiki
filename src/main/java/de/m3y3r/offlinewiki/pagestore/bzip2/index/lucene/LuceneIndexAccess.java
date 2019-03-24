package de.m3y3r.offlinewiki.pagestore.bzip2.index.lucene;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import de.m3y3r.offlinewiki.pagestore.bzip2.index.IndexAccess;
import de.m3y3r.offlinewiki.utility.SplitFile;

public class LuceneIndexAccess implements IndexAccess {

	@Override
	public List<String> getKeyAscending(int noMaxHits, String indexKey) {
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
//			logger.log(Level.SEVERE, "", e);
		}
		return null;
	}

	private File getIndexDir() {
		String id = OfflineWiki.getInstance().getConfig().getProperty("indexDir");
		File df = new File(id);
		return df;
	}

	@Override
	public List<String> getKeyAscendingLike(int maxReturnCount, String likeKey) {

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
//			logger.log(Level.SEVERE, "", e);
		}
		return null;
	}

	@Override
	public long[] getKey(String title) {

		long blockPositionInBits;
		long blockUncompressedPosition = 0;
		long pageUncompressedPosition;

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

			return new long[] { blockPositionInBits, blockUncompressedPosition, pageUncompressedPosition };
		} catch(IOException e) {
//			logger.log(Level.SEVERE, "", e);
		}
		return null;
	}
}
