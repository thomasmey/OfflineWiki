/*
 * Copyright 2012 Thomas Meyer
 */

package offlineWiki.pagestore.bzip2;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import offlineWiki.OfflineWiki;
import offlineWiki.Utf8Reader;
import offlineWiki.fileindex.entry.BlockPosition;
import offlineWiki.fileindex.entry.ComparatorBlockPosition;
import offlineWiki.fileindex.entry.ComparatorTitlePosition;
import offlineWiki.fileindex.entry.TitlePosition;
import offlineWiki.utility.HtmlUtility;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2NewBlockListener;

import common.io.objectstream.index.IndexMerger;
import common.io.objectstream.index.IndexWriter;

class Indexer implements Runnable {
	private static final int XML_BUFFER_SIZE = 1024*1024*16;

	private final File inputFile;
	private final Logger log;

	private final Comparator<TitlePosition> comparatorTitlePosition = new ComparatorTitlePosition();
	private final Comparator<BlockPosition> comparatorBlockPosition = new ComparatorBlockPosition();

	public Indexer() {
		this.inputFile = OfflineWiki.getInstance().getXmlDumpFile();
		this.log = OfflineWiki.getInstance().getLogger();
	}

	// we need to do the XML parsing ourself to get a connection between the current element file offset
	// and the parser state...
	public void run() {

		IndexWriter<TitlePosition> fileIndexWriterTitle = null;
		IndexWriter<BlockPosition> fileIndexWriterBlock = null;

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

		try {
			if(inputFile.getName().endsWith(".bz2")) {

				class BlockListener implements BZip2NewBlockListener {

					private int blockCount;
					private final IndexWriter<BlockPosition> fileIndex;

					public BlockListener(IndexWriter<BlockPosition> fileIndexBlock) {
						fileIndex = fileIndexBlock;
					}

					@Override
					public void newBlock(CompressorInputStream in, long currBlockPosition) {
						long currentFilePos = in.getBytesRead();
						fileIndex.write(new BlockPosition(currBlockPosition, currentFilePos));

						//inform about progress
						if(blockCount % 100 == 0) {
							OfflineWiki.getInstance().getLogger().log(Level.INFO,"Bzip2 block no. {2} at {0} uncompressed at {1}", new Object[] {currBlockPosition / 8, currentFilePos, blockCount});
						}

						blockCount++;
					}
				}

				fileIndexWriterBlock = new IndexWriter<BlockPosition>(inputFile, "blockPos", comparatorBlockPosition);
				BlockListener bListen = new BlockListener(fileIndexWriterBlock);

				InputStream in = new BufferedInputStream(new FileInputStream(inputFile));
				bZip2In = new BZip2CompressorInputStream(in, false, bListen);
				utf8Reader = new Utf8Reader(bZip2In);

			} else if(inputFile.getName().endsWith(".xml")) {
				utf8Reader = new Utf8Reader(new FileInputStream(inputFile));
			}

			fileIndexWriterTitle = new IndexWriter<TitlePosition>(inputFile, "titlePos", comparatorTitlePosition);

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
						log.log(Level.SEVERE,"Error! Buffer full!");
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
						fileIndexWriterTitle.write(indexEntry);
						titleCount++;
						if(titleCount % 500 == 0) {
							log.log(Level.FINE,"Processed {0} pages", titleCount);
						}
					}
				}

				// read next
				currentChar = utf8Reader.read();
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} finally {
			// close resources
			try {
				if(utf8Reader != null)
					utf8Reader.close();
			} catch (IOException e) {}
			if(fileIndexWriterTitle != null)
				fileIndexWriterTitle.close();
			if(fileIndexWriterBlock != null)
				fileIndexWriterBlock.close();
		}

		sortIndex();
	}

	public void sortIndex() {
		// step2 mergesort partial indexes
		IndexMerger<BlockPosition> im1 = null;
		IndexMerger<TitlePosition> im2 = null;
		try {
			im1 = new IndexMerger<BlockPosition>(inputFile, "blockPos", 10000, comparatorBlockPosition, false);
			im2 = new IndexMerger<TitlePosition>(inputFile, "titlePos", 10000, comparatorTitlePosition, false);
			OfflineWiki.getInstance().getThreadPool().invokeAll(Arrays.asList(im1, im2));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}