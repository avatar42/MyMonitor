package dea.monitor.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ErrDialog extends JDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected final Logger log = LoggerFactory.getLogger(getClass());
	private static final String[] possibleValues = { "Leave it", "Retry Now",
			"Mark Read" };

	private static final SimpleDateFormat fmt = new SimpleDateFormat(
			"dd-MMM-yyyy @ HH:mm:ss");
	public static final String TYPE_HTML = "text/html";
	public static final String UNKNOWN = "Unknown";

	private Frame frame;
	private CheckButton cb;
	private JLabel errMsg;
	private JTextPane ep;

	ErrDialog(Frame frame, CheckButton cb) {
		super(frame, cb.getName(), false); // not modal
		log.info("max size:" + frame.getMaximumSize().getHeight() + " x "
				+ frame.getMaximumSize().getWidth());
		setMaximumSize(frame.getMaximumSize());
		setPreferredSize(frame.getSize());
		this.cb = cb;
		init();
	}

	private void setDetails(String type, String text) {
		ep.setFont(errMsg.getFont());
		ep.setForeground(errMsg.getForeground());
		ep.setBackground(errMsg.getBackground());
		if (text != null && text.contains("<html>")) {
			ep.setContentType(TYPE_HTML);
		} else {
			ep.setContentType(type);
		}

		if (text != null) {
			ep.setText(text);
		} else {
			ep.setText("Next run: " + dateToString(cb.getItem().getNextRun()));
		}

	}

	private String dateToString(GregorianCalendar gc) {
		if (gc != null) {
			return dateToString(gc.getTime());
		}
		return UNKNOWN;

	}

	private String dateToString(Date d) {
		if (d != null) {
			return fmt.format(d);
		}

		return UNKNOWN;

	}

	public void update() {
		if (cb.getLastErr() == null) {
			if (cb.getItem().getDetails() == null) {
				errMsg.setText("Last run: "
						+ dateToString(cb.getItem().getLastOK()));
				setDetails(TYPE_HTML, null);
			} else {
				errMsg.setText("Last run: "
						+ dateToString(cb.getItem().getLastOK())
						+ " Next run: "
						+ dateToString(cb.getItem().getNextRun()));
				if (cb.getItem().getContentType().toLowerCase()
						.contains("text")) {
					setDetails(TYPE_HTML, cb.getItem().getDetails());
					// clear old data
					// cb.getItem().setDetails(null);
				} else if (cb.getItem().getSavedImg() != null) {

					StyledDocument doc = (StyledDocument) ep.getDocument();

					Style style = doc.addStyle("StyleName", null);
					try {

						StyleConstants.setIcon(style, new ImageIcon(cb
								.getItem().getSavedImg()));
						doc.remove(0, doc.getLength());
						doc.insertString(doc.getLength(), "saved image", style);
					} catch (Exception e) {
						log.error("Failed to update details pane", e);
					}
				}

			}
		} else {
			errMsg.setText(cb.getLastErr() + " Next Retry:"
					+ dateToString(cb.getItem().getNextRun()));
			if (cb.getItem().getDetails() != null) {
				setDetails(cb.getItem().getContentType(), cb.getItem()
						.getDetails());
			} else {
				setDetails(TYPE_HTML, "No Details");
			}
		}
		pack();
		setVisible(true);
	}

	public void init() {
		Container pane = getContentPane();
		pane.setLayout(new BorderLayout());
		errMsg = new JLabel(cb.getLastErr());
		pane.add(errMsg, BorderLayout.NORTH);
		// Create the StyleContext, the document and the pane
		StyleContext sc = new StyleContext();
		final DefaultStyledDocument doc = new DefaultStyledDocument(sc);
		ep = new JTextPane(doc);
		ep.setEnabled(true);
		JScrollPane jsp = new JScrollPane();
		jsp.setViewportView(ep);
		pane.add(jsp, BorderLayout.CENTER);

		final JPanel buttonPanel = new JPanel();
		buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
		JButton b = new JButton(possibleValues[0]);// Cancel
		MouseListener l = new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e == null || !e.isMetaDown()) {
					setVisible(false);
				}
			}

		};
		b.addMouseListener(l);
		b.addKeyListener(ComponentHelper.getEnterClickListener(l,
				cb.getStatusLog()));
		buttonPanel.add(b);

		b = new JButton(possibleValues[1]); // Retry now
		l = new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e == null || !e.isMetaDown()) {
					cb.getItem().setNextRun(new GregorianCalendar());
					clearAndHide();
				}
			}

		};
		b.addMouseListener(l);
		b.addKeyListener(ComponentHelper.getEnterClickListener(l,
				cb.getStatusLog()));
		buttonPanel.add(b);

		b = new JButton(possibleValues[2]); // Mark read / clear data
		l = new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e == null || !e.isMetaDown()) {
					clearAndHide();
				}
			}

		};
		b.addMouseListener(l);
		b.addKeyListener(ComponentHelper.getEnterClickListener(l,
				cb.getStatusLog()));
		buttonPanel.add(b);

		pane.add(buttonPanel, BorderLayout.SOUTH);

		setSize(200, 200);
		if (frame != null) {
			setLocationRelativeTo(frame);
		}
		// Handle window closing correctly.
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowListener() {
			public void windowActivated(WindowEvent e) {
			}

			public void windowClosed(WindowEvent e) {
			}

			public void windowClosing(WindowEvent e) {
				setVisible(false);
			}

			public void windowDeactivated(WindowEvent e) {
			}

			public void windowDeiconified(WindowEvent e) {
			}

			public void windowIconified(WindowEvent e) {
			}

			public void windowOpened(WindowEvent e) {
			}
		});

		update();
	}

	/** This method clears the dialog and hides it. */
	public void clearAndHide() {
		cb.setLastErr(null);
		cb.getItem().setDetails(null);
		cb.setState(CheckButton.STATE_UNKOWN);
		setVisible(false);
	}

}