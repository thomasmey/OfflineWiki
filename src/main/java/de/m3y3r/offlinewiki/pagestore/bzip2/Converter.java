///*
// * Copyright 2017 Thomas Meyer
// */
//
//package de.m3y3r.offlinewiki.pagestore.bzip2;
//
//import java.io.BufferedInputStream;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//
//import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
//
//import de.m3y3r.offlinewiki.Utf8Reader;
//import de.m3y3r.offlinewiki.utility.HtmlUtility;
//
///**
// * Convert the bzip2 compressed wikipedia XML dumpfile
// * @author thomas
// *
// */
//class Converter implements Runnable {
//
//	private static final int XML_BUFFER_SIZE = 1024*1024*16;
//
//	private final File inputFile;
//	private final Logger logger;
//
//	public static void main(String... args) {
//		String xmlDumpFile = args[0];
//		new Converter(xmlDumpFile).run();
//	}
//
//	public Converter(String xmlDumpFile) {
//		this.inputFile = new File(xmlDumpFile);
//		this.logger = Logger.getLogger(Converter.class.getName());
//	}
//
//	// we need to do the XML parsing ourself to get a connection between the current element file offset
//	// and the parser state...
//	public void run() {
//
//		int level = 0;
//		Map<Integer,StringBuilder> levelNameMap = new HashMap<Integer,StringBuilder>();
//
//		StringBuilder sbElement = null;
//		char[] sbChar = new char[XML_BUFFER_SIZE];
//		int sbCharPos = 0;
//		long currentTagPos = 0;
//		int currentMode = 0;
//		int prevMode = 0;
//
//		int currentChar = 0;
//		Utf8Reader utf8Reader = null;
//		BZip2CompressorInputStream bZip2In = null;
//		int titleCount = 0;
//
//		try {
//			if(inputFile.getName().endsWith(".bz2")) {
//				InputStream in = new BufferedInputStream(new FileInputStream(inputFile));
//
//				bZip2In = new BZip2CompressorInputStream(in, false);
//				utf8Reader = new Utf8Reader(bZip2In);
//
//			} else if(inputFile.getName().endsWith(".xml")) {
//				utf8Reader = new Utf8Reader(new FileInputStream(inputFile));
//			}
//
//			// read first
//			currentChar = utf8Reader.read();
//
//			int cc = 0;
//			while(currentChar >= 0 && cc < 8192) {
//				System.out.print((char)currentChar);
//
//				prevMode = currentMode;
//				switch (currentMode) {
//				case 0: // characters
//
//					if(currentChar == '<') {
//						sbElement = new StringBuilder(32);
//						currentMode = 1;
//						break;
//					}
//					sbChar[sbCharPos] = (char) currentChar;
//					sbCharPos++;
//					if(sbCharPos > sbChar.length) {
//						logger.log(Level.SEVERE,"Error! Buffer full!");
//						sbCharPos=0;
//					}
//					break;
//
//				case 1: // element name open
//					if(currentChar == '/') {
//						currentMode = 2;
//						break;
//					}
//					if(currentChar == ' ') {
//						currentMode = 3;
//						break;
//					}
//					if(currentChar == '>') {
//						level++;
//						levelNameMap.put(level, sbElement);
//						sbCharPos = 0;
//						currentMode = 0;
//						break;
//					}
//					sbElement.appendCodePoint(currentChar);
//					break;
//				case 2: // element name close
//					if(currentChar == '>') {
//						levelNameMap.remove(level);
//						level--;
//						sbCharPos = 0;
//						currentMode = 0;
//						break;
//					}
//					sbElement.appendCodePoint(currentChar);
//					break;
//				case 3: // element attributes
//					if(currentChar == '"') {
//						currentMode = 5;
//						break;
//					}
//
//					if(currentChar == '/') {
//						currentMode = 4;
//						break;
//					}
//
//					if(currentChar == '>') {
//						level++;
//						levelNameMap.put(level, sbElement);
//						sbCharPos = 0;
//						currentMode = 0;
//						break;
//					}
//					break;
//				case 4: // single element
//					if(currentChar == '>') {
//						sbCharPos = 0;
//						currentMode = 0;
//						break;
//					}
//
//				case 5: // attribute assignment
//					if(currentChar == '"') {
//						currentMode = 3;
//						break;
//					}
//
//				}
//
//				if(prevMode==1) {
//					if(currentMode==0 && level == 2 && levelNameMap.get(2).toString().equals("page")) {
//						// start of <page> tag - save this position
//						currentTagPos = utf8Reader.getCurrenFilePos() - 6;
//					}
//					if(currentMode==2 && level == 3 && levelNameMap.get(2).toString().equals("page") && levelNameMap.get(3).toString().equals("title")) {
//						StringBuilder sb = new StringBuilder(256);
//						for(int i=0; i< sbCharPos; i++) {
//							sb.appendCodePoint(sbChar[i]);
//						}
//						String title = HtmlUtility.decodeEntities(sb);
//						titleCount++;
//						if(titleCount % 1000 == 0) {
//							logger.log(Level.FINE,"Processed {0} pages", titleCount);
//						}
//					}
//				}
//
//				// read next
//				currentChar = utf8Reader.read();
//				cc++;
//			}
//		} catch (IOException e) {
//			logger.log(Level.SEVERE, "failed!", e);
//		}
//	}
//}
