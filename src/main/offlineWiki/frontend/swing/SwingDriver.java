/*
 * Copyright 2012 Thomas Meyer
 */

package offlineWiki.frontend.swing;

import java.awt.EventQueue;
import java.util.logging.Level;

import javax.swing.SwingUtilities;

import offlineWiki.OfflineWiki;

public class SwingDriver implements Runnable {

	@Override
	public void run() {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						try {
							SearchWindow frame = new SearchWindow();
							frame.setVisible(true);
						} catch (Exception e) {
							OfflineWiki.getInstance().getLogger().log(Level.SEVERE,"Error in main window!", e);
						}
					}
				});
			}
		});
	}
}
