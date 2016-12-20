/*
 * Copyright 2012 Thomas Meyer
 */

package offlineWiki.frontend.swing;
import java.util.concurrent.CountDownLatch;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class SwingDriver implements Runnable {

	private final CountDownLatch countDownLatch;
	private SearchWindow searchWindow;

	public SwingDriver(CountDownLatch interactionDriverLatch) {
		countDownLatch = interactionDriverLatch;
	}

	@Override
	public void run() {
//		UIManager.syst
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				searchWindow = new SearchWindow(countDownLatch);
				searchWindow.setVisible(true);
			}
		});
	}
}
