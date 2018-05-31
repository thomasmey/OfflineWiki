package de.m3y3r.offlinewiki.utility;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import de.m3y3r.offlinewiki.Config;

public class Bzip2BlockInputStream extends InputStream {

	@Override
	public String toString() {
		return "Bzip2BlockInputStream [fromBits=" + fromBits + ", toBits=" + toBits + ", readCountBytes="
				+ readCountBytes + ", wasSeeked=" + wasSeeked + ", isBlockOverrun=" + isBlockOverrun + ", bitShift="
				+ bitShift + ", currentByte=" + currentByte + "]";
	}

	private final SplitFileInputStream in;
	private final long fromBits;
	private final long toBits;

	private long readCountBytes;
	private boolean wasSeeked;
	private boolean isBlockOverrun;
	private byte bitShift;
	private int currentByte;

	public Bzip2BlockInputStream(SplitFile bzip2File, long fromBits, long toBits) throws FileNotFoundException {
		this.fromBits = fromBits;
		this.toBits = toBits;
		this.in = new SplitFileInputStream(bzip2File, Config.SPLIT_SIZE);
	}

	@Override
	public int read() throws IOException {
		if(readCountBytes++ < 4) {
			return in.read();
		} else if(readCountBytes >= (toBits / 8)) {
			isBlockOverrun = true;
		} else if(!wasSeeked) {
			readCountBytes = fromBits / 8;
			in.seek(readCountBytes);
			bitShift = (byte) (fromBits % 8);
			currentByte = in.read();
			if(currentByte < 0)
				return -1;
			wasSeeked=true;
		}

		int rb = in.read();
		try {
			if(bitShift != 0) {
//				currentByte   1100 1100 0100 1001 1010 1101
				if(rb < 0)
					if(currentByte >= 0)
						return (currentByte << bitShift) & 0xff;
					else
						return -1;

				int b = (currentByte << bitShift | rb >>> (8 - bitShift)) & 0xff;
				return b;
			} else {
				return currentByte;
			} 
		} finally {
			currentByte = rb;
		}
	}

	public boolean isOverrun() {
		return isBlockOverrun;
	}
}