/*
 * Copyright 2012 Thomas Meyer
 */

package offlineWiki.pagestore.bzip2;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import offlineWiki.pagestore.IndexKeyFilter;
import offlineWiki.pagestore.Store;

import offlineWiki.OfflineWiki;
import offlineWiki.PageRetriever;
import offlineWiki.WikiPage;

public class BZip2Store implements Store<WikiPage, String> {

	private BZip2RandomInputStream bzin;
	private Logger logger = Logger.getLogger(BZip2Store.class.getName());
	private Connection con;

	@Override
	public void commit() { /* nop */ }

	@Override
	public void store(WikiPage wp) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean exists() {
		boolean rc = false;
		File baseFile = OfflineWiki.getInstance().getXmlDumpFile();
		if(baseFile.exists() &&
				baseFile.isFile() &&
				baseFile.getParentFile().listFiles(
						(d, fn) -> {if(fn.startsWith(baseFile.getName()) && fn.endsWith("offlinewiki.mv.db")) return true; else return false;})
				.length > 0) {

			String url = "jdbc:h2:" + baseFile.getAbsolutePath() + ".offlinewiki";

			try(Connection con = DriverManager.getConnection(url);) {
				if(con.createStatement().executeQuery("select count(*) from block_position").first() &&
						con.createStatement().executeQuery("select count(*) from title_position").first()) {
					rc = true;
				}
			} catch (SQLException e) {
				/* skip exception */
			}
		}

		return rc;
	}

	@Override
	public List<String> getIndexKeyAscending(int noMaxHits, String indexKey) {

		try (PreparedStatement psTitle = con.prepareStatement("select title, position from title_position where title >= ? order by title asc");) {
			ResultSet rsTitle = psTitle.executeQuery();
			rsTitle.setFetchSize(noMaxHits);
			List<String> resultSet = new ArrayList<>();
			while(rsTitle.next()) {
				String title = rsTitle.getString(1);
				resultSet.add(title);
			}
			return resultSet;
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "", e);
		}
		return null;
	}

	@Override
	public List<String> getIndexKeyAscending(int maxReturnCount, IndexKeyFilter<String> filter) {

		try (PreparedStatement psTitle = con.prepareStatement("select title, position from title_position order by title asc");) {
			ResultSet rsTitle = psTitle.executeQuery();
			List<String> resultSet = new ArrayList<>();
			while(resultSet.size() < maxReturnCount && rsTitle.next()) {
				String title = rsTitle.getString(1);
				if(filter.isRelevant(title) ) {
					resultSet.add(title);
				}
			}
			return resultSet;
		} catch (SQLException e) {
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

		long position = 0;

		long blockPositionInBits = 0;
		long uncompressedPosition = 0;

		// get title and offset in uncompressed stream
		try (PreparedStatement psTitle = con.prepareStatement("select title, position from title_position where title = ?)");
			 PreparedStatement psBlock = con.prepareStatement("select position_in_bits, uncompressed_position from block_position where uncompressed_position = ?)");
			) {
			psTitle.setString(1, title);
			ResultSet rsTitle = psTitle.executeQuery();
			if(rsTitle.next()) {
				position = rsTitle.getLong(2);
				psBlock.setLong(2, position);
				ResultSet rsBlock = psBlock.executeQuery();
				if(rsBlock.next()) {
					blockPositionInBits = rsBlock.getLong(1);
					uncompressedPosition = rsBlock.getLong(2);
				}
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "", e);
			return null;
		}

		try {
			synchronized (bzin) {
				//skip in the compressed bzip2 file to the given block
				bzin.skipToBlockAt(blockPositionInBits);
				// skip in the uncompressed output to the correct position
				bzin.skip(position - uncompressedPosition);
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
			String url = "jdbc:h2:" + baseFile.getAbsolutePath() + ".offlinewiki";
			con = DriverManager.getConnection(url);
		} catch (IOException | SQLException e) {
			logger.log(Level.SEVERE, "", e);
		}
	}

	@Override
	public void close() {
		try {
			if(bzin != null)
				bzin.close();
			if(con != null) {
				con.close();
			}
		} catch(IOException | SQLException ex) {
			logger.log(Level.SEVERE, "Failed!", ex);
		}
	}
}
