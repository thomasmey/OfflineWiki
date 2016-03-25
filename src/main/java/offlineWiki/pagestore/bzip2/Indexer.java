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
import java.util.logging.Level;
import java.util.logging.Logger;

import offlineWiki.OfflineWiki;
import offlineWiki.Utf8Reader;
import offlineWiki.index.entry.BlockPosition;
import offlineWiki.index.entry.TitlePosition;
import offlineWiki.utility.HtmlUtility;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2NewBlockListener;

class Indexer implements Runnable {

	private static final int XML_BUFFER_SIZE = 1024*1024*16;

	private final File inputFile;
	private final Logger logger;

	private PreparedStatement psIns;

	private int maxTitleLen;

	public Indexer() {
		this.inputFile = OfflineWiki.getInstance().getXmlDumpFile();
		this.logger = Logger.getLogger(Indexer.class.getName());
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
			if(inputFile.getName().endsWith(".bz2")) {

				class BlockListener implements BZip2NewBlockListener {

					private int blockCount;
					private PreparedStatement psIns;

					public BlockListener(Connection con) {
						try {
							psIns = con.prepareStatement("insert into block_position values (?, ?)");
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}

					@Override
					public void newBlock(CompressorInputStream in, long currBlockPosition) {
						long currentFilePos = in.getBytesRead();
						insertBlockPositionEntry(con, new BlockPosition(currBlockPosition, currentFilePos));
						//inform about progress
						if(blockCount % 100 == 0) {
							logger.log(Level.INFO,"Bzip2 block no. {2} at {0} uncompressed at {1}", new Object[] {currBlockPosition / 8, currentFilePos, blockCount});
						}

						blockCount++;
					}

					private void insertBlockPositionEntry(Connection con, BlockPosition blockPositionAvro) {
						try {
							psIns.setLong(1, blockPositionAvro.blockPositionInBits);
							psIns.setLong(2, blockPositionAvro.uncompressedPosition);
							psIns.executeUpdate();
						} catch(SQLException e) {
							e.printStackTrace();
						}
					}
				}

				con.createStatement().execute("create table block_position ( position_in_bits long, uncompressed_position long)");
				con.createStatement().execute("create table title_position ( title varchar(1024), position long )");

				con.setAutoCommit(false);

				BlockListener bListen = new BlockListener(con);

				psIns = con.prepareStatement("insert into title_position values (?, ?)");

				InputStream in = new BufferedInputStream(new FileInputStream(inputFile));
				bZip2In = new BZip2CompressorInputStream(in, false, bListen);
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
						currentMode = 		0;
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
						TitlePosition indexEntry = new TitlePosition(title, currentTagPos);
						insertIndexEntry(con, indexEntry);
						titleCount++;
						if(titleCount % 500 == 0) {
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
			con.createStatement().execute("create index title_x1 on TITLE_POSITION (TITLE asc)");
			con.createStatement().execute("create index block_position_x1 on block_position ( uncompressed_position desc)");
		} catch (IOException | SQLException e) {
			logger.log(Level.SEVERE, "failed!", e);
		}
	}

	private void insertIndexEntry(Connection con, TitlePosition titlePosition) throws SQLException {

		String title = titlePosition.title.toString();
		if(title.length() > maxTitleLen) {
			maxTitleLen = title.length();
			logger.log(Level.INFO, "Longest title \"{0}\" with size {1}", new Object[] {title, maxTitleLen});
		}

		psIns.setString(1, title);
		psIns.setLong(2, titlePosition.position);
		psIns.addBatch();
	}
}
