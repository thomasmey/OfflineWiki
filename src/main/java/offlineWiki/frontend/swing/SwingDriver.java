/*
 * Copyright 2012 Thomas Meyer
 */

package offlineWiki.frontend.swing;
import java.util.concurrent.CountDownLatch;

import javax.swing.SwingUtilities;

public class SwingDriver implements Runnable {

	private final CountDownLatch countDownLatch;
	private SearchWindow searchWindow;

	public SwingDriver(CountDownLatch interactionDriverLatch) {
		countDownLatch = interactionDriverLatch;
	}

	@Override
	public void run() {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				searchWindow = new SearchWindow(countDownLatch);
				searchWindow.setVisible(true);
			}
		});
	}
}
