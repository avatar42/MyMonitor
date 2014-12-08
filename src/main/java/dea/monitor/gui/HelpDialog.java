package dea.monitor.gui;

import java.awt.Dialog;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;

class HelpDialog extends JDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	HelpDialog(Frame frame, String title) {
		super(frame, title);
		setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);

		try {
			JEditorPane ep = new JEditorPane("file:///"
					+ new File("").getAbsolutePath() + "/uchelp.html");
			ep.setEnabled(false);
			getContentPane().add(ep);
		} catch (IOException ioe) {
			JOptionPane.showMessageDialog(frame,
					"Unable to install editor pane");
			return;
		}

		setSize(200, 200);
		setLocationRelativeTo(frame);
		setVisible(true);
	}
}