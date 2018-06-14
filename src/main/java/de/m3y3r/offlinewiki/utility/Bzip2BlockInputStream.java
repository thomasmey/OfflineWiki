package de.m3y3r.offlinewiki.utility;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.EventObject;

import de.m3y3r.offlinewiki.Config;
import de.m3y3r.offlinewiki.pagestore.bzip2.BlockFinder;
import de.m3y3r.offlinewiki.pagestore.bzip2.BlockFinderEventListener;
import de.m3y3r.offlinewiki.pagestore.bzip2.FileBasedBlockController.BlockEntry;

public class Bzip2BlockInputStream extends InputStream {

	private final ByteBuffer byteBuffer;

	/* used for bit-shifted read and write
	 * take care that when switching from read to write the below attributes changes meaning!
	 * attention double use!
	 */
	private byte bitShift;
	private int currentByte;

	private byte[] piSqrt = new byte[] { 0x17, 0x72, 0x45, 0x38, 0x50, (byte) 0x90};

	public Bzip2BlockInputStream(SplitFile bzip2File, long fromBits) throws IOException {
		this.byteBuffer = buildBlocks(bzip2File, fromBits, 1);
	}

	static private enum State { COPY_HEADER, SEEK_BLOCK, SEARCH_BLOCK, FIN, COPY_BLOCK, APPEND_END_OF_STREAM_BLOCK};

	private ByteBuffer buildBlocks(final SplitFile bzip2File, final long fromBits, final int noExtraBlocks) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate((int) Math.pow(2, 19)); // FIXME: use mean bzip2 block size * 2
		State state = State.COPY_HEADER;
		State nextState = null;
		long readCountBits = 0;
		long toBits = -1;
		int crcStream = 0;

		try(SplitFileInputStream in = new SplitFileInputStream(bzip2File, Config.SPLIT_SIZE)) {
			while(state != State.FIN) {
				switch(state) {
				case COPY_HEADER:
				{
					while(readCountBits < (8*4)) {
						int b = in.read();
						if(b >= 0) {
							bb.put((byte) b);
						} else {
							return null;
						}
						readCountBits += 8;
					}
					state = State.SEEK_BLOCK;
					nextState = State.SEARCH_BLOCK;
				}
				break;

				case SEEK_BLOCK:
				{
					in.seek(fromBits / 8);
					state = nextState;
				}
				break;

				case SEARCH_BLOCK:
				{
					BlockEntry restart = new BlockEntry(0, fromBits / 8 * 8);
					BlockFinder bf = new BlockFinder(restart);
					class NextBLockFinder implements BlockFinderEventListener {
						Long readCountBits;
						@Override
						public void onNewBlock(EventObject event, long blockNo, long readCountBits) {
							if(noExtraBlocks == blockNo) {
								this.readCountBits = readCountBits;
							}
						}
					};
					NextBLockFinder nbf = new NextBLockFinder();
					bf.addEventListener(nbf);
					while(nbf.readCountBits == null) {
						int b = in.read();
						if(b >= 0) {
							bf.onNewByte(null, b);
						} else {
							break;
						}
					}
					if(nbf.readCountBits == null) {
						/* this can happen, when we did seek to the last bzip2 block
						 * as we don't scan for the end-of-stream magic, we just overrun, the
						 * end of the stream and didn't find a next block
						 */
						//TODO: handle this situation gracefully!
						throw new IllegalStateException("TODO!");
					}

					assert nbf.readCountBits != null;

					toBits = nbf.readCountBits;
					state = State.SEEK_BLOCK;
					nextState = State.COPY_BLOCK;
				}
				break;

				case COPY_BLOCK:
				{
					bitShift = (byte) (fromBits % 8);
					readCountBits = fromBits;
					currentByte = in.read();
					if(currentByte < 0)
						return null;

					while(true) {
						int b = readShifted(in);
						if(b < 0)
							break;

						readCountBits += 8;
						if(readCountBits >= toBits) {
							state = State.APPEND_END_OF_STREAM_BLOCK;
							int bitsTooMuch = (int) (readCountBits - toBits);

							// bitshift = 3
							//           1824653 
							//                 |
							//                 |
							// 0xff      0x59      0x8a
							// 1111 1111 0101 1001 1000 1010
							//                 001 1000 1 = 0x31
							bitShift = (byte) bitsTooMuch;

							if(bitsTooMuch > 0) {
								int prevByte = b;
//								int prevByte = bb.get(bb.position() - 1);
//								bb.position(bb.position() - 1);
								{
									int v031 = (prevByte << (8 - bitsTooMuch)) & 0xff;
									int sb031 = (0x31 >>> (8 - bitsTooMuch)) << (8 - bitsTooMuch) & 0xff;
									if(v031 != sb031) {
										System.out.println("help; is="+ v031 + "should be=" + sb031 + "shfit=" + bitsTooMuch + "readCountBits="+readCountBits);
									}
								}
								currentByte = (prevByte >>> bitShift) & 0xff;
								break;
							}
							// bitshift is zero, write current byte to buffer and end
							bb.put((byte) b);
							break;
						}
						bb.put((byte) b);
					}
				}
				break;

				case APPEND_END_OF_STREAM_BLOCK:
				{
					for(byte b: piSqrt)
						bb.put((byte) shiftByte(b & 0xff));
					for(byte b: toByteArray(crcStream))
						bb.put((byte) shiftByte(b & 0xff));
					bb.put((byte) shiftByte(-1));
					state = State.FIN;
				}
				break;
				}
			}
		}
		bb.flip();
		return bb;
	}

	private byte[] toByteArray(int value) {
		return new byte[] {
				(byte)(value >>> 24),
				(byte)(value >>> 16),
				(byte)(value >>> 8),
				(byte)value
		};
	}

	private int readShifted(SplitFileInputStream in) throws IOException {
		int nextByte = in.read();
		return shiftByte(nextByte);
	}

	private int shiftByte(int nextByte) {
		try {
			if(bitShift == 0) {
				return currentByte;
			}

			if(nextByte < 0) { // we did hit end of stream, process buffered bits
				if(currentByte >= 0)
					return (currentByte << bitShift) & 0xff;
				else
					return -1;
			}
			int b = (currentByte << bitShift | nextByte >>> (8 - bitShift)) & 0xff;
			return b;
		} finally {
			currentByte = nextByte;
		}
	}

	@Override
	public int read() throws IOException {
		if(byteBuffer.hasRemaining()) {
			return byteBuffer.get() & 0xff;
		} else {
			return -1;
		}
	}
}