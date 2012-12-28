package offlineWiki.frontend.swing;

import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.JTextPane;

import offlineWiki.WikiPage;
import javax.swing.JScrollPane;

public class WikiPageWindow extends JFrame {

	private final JPanel contentPane;
	private final JScrollPane scrollPane;
	private final JTextPane textPane;

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

		scrollPane = new JScrollPane();
		contentPane.add(scrollPane, BorderLayout.CENTER);

		textPane = new JTextPane();
		textPane.setEditable(false);
		String text = page.getText();
		textPane.setText(text);

		// reset scroll bars...
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				scrollPane.setViewportView(textPane);
				int miny = scrollPane.getVerticalScrollBar().getMinimum();
				scrollPane.getVerticalScrollBar().setValue(miny);
				int minx = scrollPane.getHorizontalScrollBar().getMinimum();
				scrollPane.getHorizontalScrollBar().setValue(minx);
			}
		});

		textPane.addKeyListener(new KeyAdapter() {

			@Override
			public void keyReleased(KeyEvent e) {
				if(e.isConsumed())
					return;

				if(e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					WikiPageWindow.this.dispose();
					e.consume();
				}
			}
		});
	}

}
