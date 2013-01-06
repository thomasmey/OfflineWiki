package offlineWiki.index.entry;

public class TitlePosition {
	public final java.lang.CharSequence title;
	public final long position;

	public TitlePosition(java.lang.CharSequence title, java.lang.Long position) {
		this.title = title;
		this.position = position;
	}

	public java.lang.CharSequence getTitle() {
		return title;
	}

	public long getPosition() {
		return position;
	}
}
