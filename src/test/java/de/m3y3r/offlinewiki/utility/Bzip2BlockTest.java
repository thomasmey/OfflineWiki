package de.m3y3r.offlinewiki.utility;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

public class Bzip2BlockTest {

	public static void main(String[] args) throws FileNotFoundException, IOException {
//		String file = "block-0.bz2";
		String file = "dewiki-latest-pages-articles.xml.bz2.0";

		try(FileInputStream fis = new FileInputStream(file);
			BZip2CompressorInputStream bis = new BZip2CompressorInputStream(fis)) {
			int b = bis.read();
			while(b >= 0) {
				b = bis.read();
			}
		}
	}

}
