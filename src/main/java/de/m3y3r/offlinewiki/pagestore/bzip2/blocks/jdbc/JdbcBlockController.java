package de.m3y3r.offlinewiki.pagestore.bzip2.blocks.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;

import de.m3y3r.offlinewiki.pagestore.bzip2.BlockEntry;
import de.m3y3r.offlinewiki.pagestore.bzip2.BlockEntry.IndexState;
import de.m3y3r.offlinewiki.pagestore.bzip2.blocks.BlockController;
import de.m3y3r.offlinewiki.pagestore.bzip2.blocks.BlockFinderEventListener;
import de.m3y3r.offlinewiki.utility.Database;

public class JdbcBlockController implements BlockController, BlockFinderEventListener {

	private static final String SQL_UPDATE = "update blocks set index_state = ? where block_no = ?";
	private static final String SQL_FETCH_LAST = "select block_no, block_start_bits from blocks where block_no = ( select max(block_no) from blocks)";
	private static final String SQL_INSERT = "insert into blocks values (?, ?, ?)";

	private static final int MAX_ENTRIES = 50;

	private final List<BlockEntry> entries;

	public JdbcBlockController() {
		entries = new ArrayList<BlockEntry>();
		createTables();
	}

	private void createTables() {
		try(Connection c = Database.getConnection()) {
			String sqlCreateBlocks =
					"create table if not exists  blocks (" +
							"    block_no         long not null, " +
							"    block_start_bits long not null, " +
							"    index_state      integer not null default 0," +
							"  primary key (block_no) )";

			c.createStatement().execute(sqlCreateBlocks);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onNewBlock(EventObject event, long blockNo, long readCountBits) {
		BlockEntry entry = new BlockEntry(blockNo, readCountBits);
		entries.add(entry);
		if(entries.size() > MAX_ENTRIES) {
			flush();
		}
	}

	private void flush() {
		try(Connection c = Database.getConnection()) {
			try(PreparedStatement ps = c.prepareStatement(SQL_INSERT)) {
				for(BlockEntry e: entries) {
					ps.setLong(1, e.blockNo);
					ps.setLong(2, e.readCountBits);
					ps.setInt(3, e.indexState == null ? IndexState.INITIAL.ordinal() : e.indexState.ordinal());
					ps.addBatch();
				}
				int[] result = ps.executeBatch();
				System.out.println("result=" + Arrays.toString(result));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			entries.clear();
		}
	}

	@Override
	public void onEndOfFile(EventObject event) {
		flush();
	}

	@Override
	public BlockEntry getLatestEntry() {
		try(Connection c = Database.getConnection()) {
			try(PreparedStatement ps = c.prepareStatement(SQL_FETCH_LAST)) {
				try(ResultSet rs = ps.executeQuery()) {
					if(rs.next()) {
						return new BlockEntry(rs.getLong(1), rs.getLong(2));
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Iterator<BlockEntry> getBlockIterator() {
		return new BlockIterator();
	}

	@Override
	public void setBlockFinished(long blockNo) {
		try(Connection c = Database.getConnection()) {
			try(PreparedStatement ps = c.prepareStatement(SQL_UPDATE)) {
				ps.setLong(2, blockNo);
				ps.setInt(1, IndexState.FINISHED.ordinal());
				int rows = ps.executeUpdate();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}

class BlockIterator implements Iterator<BlockEntry> {

	private static final String SQL_FETCH_ALL = "select block_no, block_start_bits, index_state from blocks order by block_no asc";
	private final ResultSet rs;

	public BlockIterator() {
		try {
			Connection c = Database.getConnection();
			PreparedStatement ps = c.prepareStatement(SQL_FETCH_ALL);
			ResultSet rs = ps.executeQuery();
			this.rs = rs;
		} catch(SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean hasNext() {
		boolean hasNext = false;
		try {
			hasNext = rs.next();
			if(!hasNext) {
				rs.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return hasNext;
	}

	@Override
	public BlockEntry next() {
		try {
			return new BlockEntry(rs.getLong(1), rs.getLong(2));
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
}