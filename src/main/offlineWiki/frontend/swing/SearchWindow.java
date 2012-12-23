package offlineWiki.frontend.swing;

import java.awt.BorderLayout;
import java.awt.Point;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.JTextField;
import javax.swing.JList;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

import offlineWiki.OfflineWiki;
import offlineWiki.WikiPage;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SearchWindow extends JFrame {

	private final JPanel contentPane;
	private final JTextField searchField;
	private final JList<String> searchResultList;
	private final SearchResultListModel searchResultListModel;
	private JScrollBar scrollBar;
	private JScrollPane scrollPane;

	/**
	 * Create the frame.
	 */
	public SearchWindow() {
		setTitle("Search");
		this.searchResultListModel = new SearchResultListModel();

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));

		searchField = new JTextField();
		searchField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				if(!e.isActionKey()) {
					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {
							searchResultListModel.updateSearchResultList(searchField.getText());
						}
					});
				} else {
					searchResultList.transferFocus();
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
					Point p = e.getPoint();
					final String title = searchResultList.getSelectedValue();
					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {
							WikiPage page = OfflineWiki.getInstance().getPageStore().retrieveByTitel(title);
							WikiPageWindow window = new WikiPageWindow(page);
							window.setVisible(true);
						}
						
					});
				}
			}
		});
		searchResultList.setModel(searchResultListModel);

		scrollPane = new JScrollPane(searchResultList);
		contentPane.add(scrollPane, BorderLayout.CENTER);
	}

}
