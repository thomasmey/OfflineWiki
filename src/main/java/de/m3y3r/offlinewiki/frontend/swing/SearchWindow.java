package de.m3y3r.offlinewiki.frontend.swing;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import de.m3y3r.offlinewiki.OfflineWiki;
import de.m3y3r.offlinewiki.WikiPage;

import javax.swing.JTextField;
import javax.swing.JList;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.CountDownLatch;

public class SearchWindow extends JFrame {

	private final JPanel contentPane;
	private final JTextField searchField;
	private final JList<String> searchResultList;
	private final SearchResultListModel searchResultListModel;
	private final CountDownLatch countDownLatch;
	private final JScrollPane scrollPane;

	private JScrollBar scrollBar;

	/**
	 * Create the frame.
	 */
	public SearchWindow(CountDownLatch countDownLatch) {

		super();

		this.countDownLatch = countDownLatch;

		setTitle("Search");
		this.searchResultListModel = new SearchResultListModel();

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosed(WindowEvent e) {
				SearchWindow.this.countDownLatch.countDown();
			}
		});

		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));

		searchField = new JTextField();
		searchField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				if(e.isConsumed())
					return;

				if(e.getKeyChar() == '\n') {

					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {
							String searchText = searchField.getText();
							searchResultListModel.updateSearchResultList(searchText);
//							int min = scrollPane.getVerticalScrollBar().getMinimum();
//							scrollPane.getVerticalScrollBar().setValue(min);
							searchResultList.setSelectedValue(searchText, true);
						}
					});
					searchField.transferFocus();
					e.consume();
				}
			}
		});

		contentPane.add(searchField, BorderLayout.NORTH);
		searchField.setColumns(10);

		searchResultList = new JList<String>();
		searchResultList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount() == 2) {
					String title = searchResultList.getSelectedValue();
					OfflineWiki.getInstance().getThreadPool().execute(new ResultViewer(title));
					e.consume();
				}
			}
		});

		searchResultList.addKeyListener(new KeyAdapter() {

			@Override
			public void keyTyped(KeyEvent e) {
				if(e.isConsumed())
					return;

				char keyChar = e.getKeyChar();
				if(keyChar == '\n' || keyChar == ' ') {
					String title = searchResultList.getSelectedValue();
					OfflineWiki.getInstance().getThreadPool().execute(new ResultViewer(title));
					e.consume();
				}
			}
		});

		searchResultList.setModel(searchResultListModel);

		scrollPane = new JScrollPane(searchResultList);
		contentPane.add(scrollPane, BorderLayout.CENTER);
	}
}

/** retrieves a WikiPage and displays the result, can run on any thread */
class ResultViewer implements Runnable {

	private final String title;
	public ResultViewer(String title) {
		this.title = title;
	}

	@Override
	public void run() {
		final WikiPage page = OfflineWiki.getInstance().getPageStore().retrieveByIndexKey(title);
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				WikiPageWindow window = new WikiPageWindow(page);
				window.setVisible(true);
			}
		});
	}

}
