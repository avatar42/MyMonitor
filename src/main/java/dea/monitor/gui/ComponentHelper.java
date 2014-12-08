/**
 * 
 */
package dea.monitor.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

/**
 * 
 * Helper class for standardizing components.
 * 
 * 
 * @author deabigt
 * 
 */
public class ComponentHelper {
	public static TitledBorder getBorder(String labelText) {
		TitledBorder bline = BorderFactory.createTitledBorder(BorderFactory
				.createLineBorder((Colors) UIManager
						.get("TitledBorder.foreground")), labelText);
		bline.setTitleColor((Color) UIManager.get("TitledBorder.titleColor"));
		bline.setTitleFont((Font) UIManager.get("TitledBorder.font"));
		bline.setTitleJustification(TitledBorder.RIGHT);
		return bline;
	}

	/**
	 * Get a KeyAdapter that listens for the Enter key and clicks th button
	 * mouseListener is linked to when it sees it.
	 * 
	 * @param mouseListener
	 *            MouseListener of button
	 * @param appDataPanel
	 * 
	 * @return KeyAdapter
	 */
	public static KeyAdapter getEnterClickListener(
			final MouseListener mouseListener, final ScrollingLogPane statusLog) {
		KeyAdapter ka = new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				char c = e.getKeyChar();
				if (c == KeyEvent.VK_ENTER) {
					if (mouseListener != null) {
						statusLog.logIt(ScrollingLogPane.SHOW_INFO,
								"Converting enter / space to click:"
										+ mouseListener.toString());
						mouseListener.mousePressed(null);
					}
				}
			}

		};
		return ka;
	}

}
