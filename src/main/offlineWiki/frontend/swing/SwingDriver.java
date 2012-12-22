/*
 * Copyright 2012 Thomas Meyer
 */

package offlineWiki.frontend.swing;

import java.awt.EventQueue;

import javax.swing.SwingUtilities;

import offlineWiki.frontend.console.MainWindow;

public class SwingDriver implements Runnable {

	@Override
	public void run() {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				MainWindow mw = new MainWindow();
				mw.pack();
			}
		});
	}
}
