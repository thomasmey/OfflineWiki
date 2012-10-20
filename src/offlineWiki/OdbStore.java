package offlineWiki;

import java.io.File;
import java.util.SortedSet;

import com.db4o.Db4oEmbedded;
import com.db4o.ObjectContainer;
import com.db4o.config.EmbeddedConfiguration;
import com.db4o.query.Predicate;

class OdbStore implements PageStore<WikiPage> {

	private ObjectContainer container;

	private static class PredicateCeilingPages extends Predicate<WikiPage> {

		private static final long serialVersionUID = 5413470103218514543L;
		private int counter;
		private final int maxCounter;
		private final String searchTitle;

		public PredicateCeilingPages(String substring, int i) {
			this.maxCounter = i;
			this.searchTitle = substring;
		}

		@Override
		public boolean match(WikiPage wp) {
			String title = wp.getTitle();
			int i = title.compareTo(searchTitle);
			if (i == 0) {
				// exact macth
				return true;
			}
			return false;
		}
		
	}

	public OdbStore() {
		File baseFileName = OfflineWiki.getInstance().getXmlDumpFile();
		File databaseFile = new File(baseFileName + ".db4o");
		if(databaseFile.exists()) {
			ObjectContainer container = null;
			EmbeddedConfiguration configuration = Db4oEmbedded.newConfiguration();
			configuration.file().blockSize(8);
			container = Db4oEmbedded.openFile(configuration, databaseFile+"");
		}
//		 else {
//				// open database
//				ObjectContainer container = null;
//				EmbeddedConfiguration configuration = Db4oEmbedded.newConfiguration();
//				configuration.file().blockSize(8);
//				container = Db4oEmbedded.openFile(configuration, databaseFile.getAbsolutePath());
//				pageStore = new OdbStore(container);
//			}
	}

	@Override
	public void commit() {
		container.commit();
	}

	@Override
	public void store(WikiPage wp) {
		container.store(wp);
	}

	@Override
	public void close() {
		container.commit();
		container.close();
	}

	@Override
	public SortedSet<WikiPage> getTitleAscending(String title, int noMaxHits) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean exists() {
		// TODO Auto-generated method stub
		return false;
	}

}
