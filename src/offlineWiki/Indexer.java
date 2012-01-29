/*
 * Copyright (C) 2012 Thomas Meyer
 */

package offlineWiki;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class Indexer {
	
	private RandomAccessFile raf;
	private byte[] readBuffer = new byte[1024*1024*16];
	private int readBufferPos;
	private int readBufferLength;

//	private Map<String,WikiPage> articleMap = new HashMap<String,WikiPage>();
	private TreeMap<String,Long> articleIndexTitle = new TreeMap<String,Long>();
	private long currenFilePos;

	public Indexer(String filename) throws IOException, ClassNotFoundException {
		
		File fIndex = new File(filename + ".index");
		if(fIndex.exists()) {
			loadIndex(fIndex);
		} else {
			System.out.println("Index file not found!");
			raf = new RandomAccessFile(filename, "r");
			// index wikipage titles and their file offsets
			createIndex();
			storeIndex(fIndex);
		}

	}

	private void storeIndex(File fIndex) throws FileNotFoundException, IOException {
		System.out.println("Storing index.");
		ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(fIndex)));
		out.writeObject(articleIndexTitle);
		out.close();
		System.out.println("Index size: " + articleIndexTitle.size());
	}

	private void loadIndex(File fIndex) throws IOException, ClassNotFoundException {
		System.out.println("Loading index.");
		ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(fIndex)));
		
		articleIndexTitle = (TreeMap<String, Long>) in.readObject();
		in.close();
		System.out.println("Index size: " + articleIndexTitle.size());
	}

	// this implements an easy get next utf-8 char method...
	// JAVA Y U N PROVIDE THIS?
	private int readUTF8() throws IOException {
		
		int rc = 0;
		int b = read();
		if(b < 0)
			throw new EOFException();
		
		if((b >> 7 ) == 0) {
			// single byte UTF-8
			rc = (b & 0x7f);
		} else {
			// multi byte
			int mask = 0;
			int count = 0;

			if((b >> 5) == 0x06) {
				mask = 0x1f;
				count = 1;
			} else
				if((b >> 4) == 0xe) {
					mask = 0x0f;
					count = 2;
				} else
					if((b >> 3) == 0x1e) {
						mask = 0x07;
						count = 3;
					}

			rc = b & mask;
			for(int i = 0; i < count; i++) {
				rc = rc << 6;
				b = read();
				if(b < 0)
					throw new EOFException();
				if((b >> 6) == 2) {
					rc = (rc | (b & 0x3f));
				} else
					throw new RuntimeException("Invalid UTF-8 sequence!");
			}

		}
		return rc;
	}

	// we need to do the buffering ourself
	private int read() throws IOException {
		
		int rc = -1;
		if(readBufferLength==0 || readBufferPos > readBufferLength - 1) {
			readBufferLength = raf.read(readBuffer);
			readBufferPos=0;
		}
		if(readBufferLength<0)
			return rc;
		
		rc = (int)readBuffer[readBufferPos] & 0xff;
		readBufferPos++;
		currenFilePos++;
		if(currenFilePos % (1024*1024*128) == 0)
			System.out.println("FilePos: " + currenFilePos + " index size: " + articleIndexTitle.size());
		return rc;
	}

	// we need to do the xml parsing ourself to get a connection between the current element file offset
	// and the parser state...
	public void createIndex() throws IOException {

		System.out.println("Creating index.");
		int level = 0;
		Map<Integer,StringBuilder> levelNameMap = new HashMap<Integer,StringBuilder>();

		StringBuilder sbElement = null;
		int[] sbChar = new int[1024*1024*16];
		int sbCharPos = 0;
		long currentTagPos = 0;
		int currentMode = 0;
		int prevMode = 0;

		boolean eof = false;
		int currentChar = 0;

		// vorlesen
		try {
			currentChar = readUTF8();
		} catch (EOFException e) {
			eof = true;
		}

		while(!eof) {

			prevMode = currentMode;
			switch (currentMode) {
			case 0: // characters
				
				if(currentChar == '<') {
					sbElement = new StringBuilder(32);
					currentMode = 1;
					break;
				}
				sbChar[sbCharPos] = currentChar;
				sbCharPos++;
				if(sbCharPos > sbChar.length) {
					System.out.println("Error! Buffer full!");
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
					currentTagPos = currenFilePos - 6;
				}
				if(currentMode==2 && level == 3 && levelNameMap.get(2).toString().equals("page") && levelNameMap.get(3).toString().equals("title")) {
					StringBuilder sb = new StringBuilder(256);
					for(int i=0; i< sbCharPos; i++) {
						sb.appendCodePoint(sbChar[i]);
					}
					String test = sb.toString();
					articleIndexTitle.put(test, currentTagPos);
				}
			}
			// nachlesen
			try {
				currentChar = readUTF8();
			} catch (EOFException e) {
				eof = true;
			}
		}

		raf.close();
	}

	public TreeMap<String,Long> getArticleIndexTitle() {
		return articleIndexTitle;
	}

}
