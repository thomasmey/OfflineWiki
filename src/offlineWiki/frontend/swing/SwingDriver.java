/*
 * Copyright 2012 Thomas Meyer
 */

package offlineWiki.frontend.swing;

import java.util.TreeMap;

import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import offlineWiki.PageRetriever;

class SwingDriver implements Runnable {

	private TreeMap<String,Long> articleIndexTitle;
	private PageRetriever pageRetriever;

	private JFrame frame;
	private JTextField textField;
	private JList<String> resultList;
	private JScrollPane scrollPane;

	public SwingDriver(TreeMap<String, Long> articleIndexTitle, PageRetriever pr) {
		this.pageRetriever = pr;
		this.articleIndexTitle = articleIndexTitle;
	}

	@Override
	public void run() {

	}

	
}
