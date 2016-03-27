/*
 * Copyright 2012 Thomas Meyer
 */

package offlineWiki.pagestore.bzip2;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import offlineWiki.OfflineWiki;
import offlineWiki.Utf8Reader;
import offlineWiki.utility.HtmlUtility;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

class Indexer implements Runnable {

	private static final int XML_BUFFER_SIZE = 1024*1024*16;

	private final File inputFile;
	private final Logger logger;

	private PreparedStatement psIns;

	private int maxTitleLen;

	/** bzip2 stream mapping: block starts: uncompressed position, position in bits*/
	private TreeMap<Long,Long> bzip2Blocks;

	/** current bzip2 block number */
	private long blockCount;

	public Indexer() {
		this.inputFile = OfflineWiki.getInstance().getXmlDumpFile();
		this.logger = Logger.getLogger(Indexer.class.getName());
		this.bzip2Blocks = new TreeMap<>();
	}

	// we need to do the XML parsing ourself to get a connection between the current element file offset
	// and the parser state...
	public void run() {

		String url = "jdbc:h2:" + inputFile.getAbsolutePath() + ".offlinewiki";

		int level = 0;
		Map<Integer,StringBuilder> levelNameMap = new HashMap<Integer,StringBuilder>();

		StringBuilder sbElement = null;
		char[] sbChar = new char[XML_BUFFER_SIZE];
		int sbCharPos = 0;
		long currentTagPos = 0;
		int currentMode = 0;
		int prevMode = 0;

		int currentChar = 0;
		Utf8Reader utf8Reader = null;
		BZip2CompressorInputStream bZip2In = null;
		int titleCount = 0;

		try(Connection con = DriverManager.getConnection(url)) {
			con.createStatement().execute("create table title_position ( page_title varchar(1024), page_uncompressed_position long, block_uncompressed_position long, block_position_in_bits long)");
			con.createStatement().execute("create index title_position_x1 on title_position (page_title asc)");

//			con.createStatement().execute("create table index_status ()

			con.setAutoCommit(false);
			psIns = con.prepareStatement("insert into title_position values (?, ?, ?, ?)");

			if(inputFile.getName().endsWith(".bz2")) {
				InputStream in = new BufferedInputStream(new FileInputStream(inputFile));

				bZip2In = new BZip2CompressorInputStream(in, false, (lin, blockPositionInBits) -> 
				{	long blockUncompressedPosition = lin.getBytesRead();
					if(blockCount % 100 == 0) {
						logger.log(Level.INFO,"Bzip2 block no. {2} at {0} uncompressed at {1}", new Object[] {blockPositionInBits / 8, blockUncompressedPosition, blockCount});
					}
					synchronized (bzip2Blocks) {
						bzip2Blocks.put(blockUncompressedPosition, blockPositionInBits);
					}
					blockCount++;
				});
				utf8Reader = new Utf8Reader(bZip2In);

			} else if(inputFile.getName().endsWith(".xml")) {
				utf8Reader = new Utf8Reader(new FileInputStream(inputFile));
			}

			// read first
			currentChar = utf8Reader.read();

			while(currentChar >= 0) {

				prevMode = currentMode;
				switch (currentMode) {
				case 0: // characters

					if(currentChar == '<') {
						sbElement = new StringBuilder(32);
						currentMode = 1;
						break;
					}
					sbChar[sbCharPos] = (char) currentChar;
					sbCharPos++;
					if(sbCharPos > sbChar.length) {
						logger.log(Level.SEVERE,"Error! Buffer full!");
						sbCharPos=0;
					}
					break;

				case 1: // element name open
					if(currentChar == '/') {
						currentMode = 2;
						break;
					}
					if(currentChar == ' ') {
						currentMode = 3;
						break;
					}
					if(currentChar == '>') {
						level++;
						levelNameMap.put(level, sbElement);
						sbCharPos = 0;
						currentMode = 0;
						break;
					}
					sbElement.appendCodePoint(currentChar);
					break;
				case 2: // element name close
					if(currentChar == '>') {
						levelNameMap.remove(level);
						level--;
						sbCharPos = 0;
						currentMode = 0;
						break;
					}
					sbElement.appendCodePoint(currentChar);
					break;
				case 3: // element attributes
					if(currentChar == '"') {
						currentMode = 5;
						break;
					}

					if(currentChar == '/') {
						currentMode = 4;
						break;
					}

					if(currentChar == '>') {
						level++;
						levelNameMap.put(level, sbElement);
						sbCharPos = 0;
						currentMode = 0;
						break;
					}
					break;
				case 4: // single element
					if(currentChar == '>') {
						sbCharPos = 0;
						currentMode = 0;
						break;
					}

				case 5: // attribute assignment
					if(currentChar == '"') {
						currentMode = 3;
						break;
					}

				}

				if(prevMode==1) {
					if(currentMode==0 && level == 2 && levelNameMap.get(2).toString().equals("page")) {
						// save position
						currentTagPos = utf8Reader.getCurrenFilePos() - 6;
					}
					if(currentMode==2 && level == 3 && levelNameMap.get(2).toString().equals("page") && levelNameMap.get(3).toString().equals("title")) {
						StringBuilder sb = new StringBuilder(256);
						for(int i=0; i< sbCharPos; i++) {
							sb.appendCodePoint(sbChar[i]);
						}
						String title = HtmlUtility.decodeEntities(sb);
						insertIndexEntry(con, title, currentTagPos);
						titleCount++;
						if(titleCount % 1000 == 0) {
							logger.log(Level.FINE,"Processed {0} pages", titleCount);
							psIns.executeBatch();
							con.commit();
						}
					}
				}

				// read next
				currentChar = utf8Reader.read();
			}
			psIns.executeBatch();
			logger.log(Level.INFO, "creating DB index!");
		} catch (IOException | SQLException e) {
			logger.log(Level.SEVERE, "failed!", e);
		}
	}

	private void insertIndexEntry(Connection con, String pageTitel, long currentTagUncompressedPosition) throws SQLException {

		String title = pageTitel;
		if(title.length() > maxTitleLen) {
			maxTitleLen = title.length();
			logger.log(Level.INFO, "Longest title \"{0}\" with size {1}", new Object[] {title, maxTitleLen});
		}

		long blockUncompressedPosition;
		long blockPositionInBits;
		synchronized (bzip2Blocks) {
			Entry<Long, Long> e = bzip2Blocks.floorEntry(currentTagUncompressedPosition);
			blockUncompressedPosition = e.getKey();
			blockPositionInBits = e.getValue();

			// remove all smaller entries from map
			Long lowerKey;
			while ((lowerKey = bzip2Blocks.lowerKey(e.getKey())) != null) {
				bzip2Blocks.remove(lowerKey);
			}
		}

		psIns.setString(1, title);
		psIns.setLong(2, currentTagUncompressedPosition);
		psIns.setLong(3, blockUncompressedPosition);
		psIns.setLong(4, blockPositionInBits);
		psIns.addBatch();
	}
}
