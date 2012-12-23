package offlineWiki.frontend.swing;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JTextPane;

import offlineWiki.WikiPage;
import javax.swing.JScrollPane;

public class WikiPageWindow extends JFrame {

	private JPanel contentPane;
	private final WikiPage page;

	/**
	 * Create the frame.
	 */
	public WikiPageWindow(WikiPage page) {
		this.page = page;

		setTitle(page.getTitle());

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);

		JScrollPane scrollPane = new JScrollPane();
		contentPane.add(scrollPane, BorderLayout.CENTER);

		JTextPane textPane = new JTextPane();
		textPane.setEditable(false);
		String text = page.getText();
		textPane.setText(text);

		scrollPane.setViewportView(textPane);
	}

}
