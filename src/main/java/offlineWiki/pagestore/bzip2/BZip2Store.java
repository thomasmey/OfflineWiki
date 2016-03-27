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

import offlineWiki.pagestore.Store;

import offlineWiki.OfflineWiki;
import offlineWiki.PageRetriever;
import offlineWiki.WikiPage;

public class BZip2Store implements Store<WikiPage, String> {

	private BZip2RandomInputStream bzin;
	private Logger logger = Logger.getLogger(BZip2Store.class.getName());
	private Connection con;

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
				e.printStackTrace();
			}
		}

		return rc;
	}

	@Override
	public List<String> getIndexKeyAscending(int noMaxHits, String indexKey) {

		try (PreparedStatement psTitle = con.prepareStatement("select page_title from title_position where page_title >= ? order by page_title asc limit ?" );) {
			psTitle.setString(1, indexKey);
			psTitle.setInt(2, noMaxHits);
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
	public List<String> getIndexKeyAscendingLike(int maxReturnCount, String likeKey) {

		try (PreparedStatement psTitle = con.prepareStatement("select page_title from title_position where page_title like ? order by page_title asc limit ?");) {
			psTitle.setString(1, likeKey);
			psTitle.setInt(2, maxReturnCount);
			ResultSet rsTitle = psTitle.executeQuery();
			List<String> resultSet = new ArrayList<>();
			while(resultSet.size() < maxReturnCount && rsTitle.next()) {
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
		try (PreparedStatement psTitle = con.prepareStatement("select page_title, page_uncompressed_position, block_uncompressed_position, block_position_in_bits from title_position where page_title = ?")) {
			psTitle.setString(1, title);
			ResultSet rsTitle = psTitle.executeQuery();
			if(rsTitle.next()) {
				pageUncompressedPosition = rsTitle.getLong(2);
				blockUncompressedPosition = rsTitle.getLong(3);
				blockPositionInBits = rsTitle.getLong(4);
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
