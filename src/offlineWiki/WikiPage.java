/*
 * Copyright (C) 2012 Thomas Meyer
 */

package offlineWiki;

import java.util.Date;
import java.util.List;

public class WikiPage {

	private String title;
	private long id;
//	private WikiRevision revision;
	private long revId;
	private Date timestamp;
	private List<String> contributorIp;
	private String comment;
	private String text;
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public long getRevId() {
		return revId;
	}
	public void setRevId(long revId) {
		this.revId = revId;
	}
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	public List<String> getContributorIp() {
		return contributorIp;
	}
	public void setContributorIp(List<String> contributorIp) {
		this.contributorIp = contributorIp;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
}
