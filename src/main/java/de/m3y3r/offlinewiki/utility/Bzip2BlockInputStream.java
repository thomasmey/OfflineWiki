package de.m3y3r.offlinewiki.utility;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javax.swing.ComponentInputMap;

import de.m3y3r.offlinewiki.Config;

public class Bzip2BlockInputStream extends InputStream {

	private final ByteBuffer byteBuffer;

	private byte bitShift;
	private int currentByte;
	private int[] endOfStream = new int[] { 0x17, 0x72, 0x45, 0x35, 0x50, 0x90, 0, 0, 0, 0, 0, -1};

	public Bzip2BlockInputStream(SplitFile bzip2File, long fromBits) throws IOException {
		this.byteBuffer = buildBlocks(bzip2File, fromBits, 2);
	}

	private ByteBuffer buildBlocks(SplitFile bzip2File, long fromBits, int noBlocks) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(1_000_000); // FIXME
		try(SplitFileInputStream in = new SplitFileInputStream(bzip2File, Config.SPLIT_SIZE)) {
			
		}
	}

	@Override
	public int read() throws IOException {
		int rb = -1;

		if(endStream) { // end of stream, supply artificial eos header: magic, crc, filler
			rb = endOfStream[(int) readCountBytes];
		} else if(readCountBytes < 4) {
			currentByte = in.read();
		} else if(readCountBytes >= (toBits / 8)) {
			isBlockOverrun = true;
		} else {
			if(!wasSeeked) {
				readCountBytes = fromBits / 8;
				in.seek(readCountBytes);
				bitShift = (byte) (fromBits % 8);
				currentByte = in.read();
				if(currentByte < 0)
					return -1;
				wasSeeked=true;
			}
			rb = in.read();
		}

		readCountBytes++;
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
}