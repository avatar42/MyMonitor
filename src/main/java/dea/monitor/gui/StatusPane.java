package dea.monitor.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Date;
import java.util.Enumeration;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatusPane extends JTextPane {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected final Logger log = LoggerFactory.getLogger(getClass());
	private String name = "Monitor Status Log";
	private int lines = 0;
	private int total = 0;
	private int max = 0;
	private Border focusBorder;
	private Border unfocusBorder;
	private String lastUser = "";
	private StyleContext sc;
	private Style heading2Style;
	private Style speechStyle;
	private Style simpleStyle;
	private int bottom = 0;
	private int scrollMax = 0;
	private int docLen = 0;
	private JScrollPane scrollPane = null;
	// lines written from the same user
	private int sameLines = 0;
	private Color hgbc = (Color) UIManager.get("Panel.highlight.background");
	private Color hgfc = (Color) UIManager.get("Panel.highlight.foreground");

	public StatusPane() {
		setName(name);
		setAutoscrolls(false);
		setFocusTraversalKeysEnabled(true);
		sc = new StyleContext();
		DefaultStyledDocument doc = new DefaultStyledDocument(sc);
		setDocument(doc);
		// StyledEditorKit ek = new StyledEditorKit();
		// setEditorKit(ek);
		getAccessibleContext().setAccessibleDescription(
				name + " info found here.");

		unfocusBorder = new EmptyBorder(3, 3, 3, 3);
		focusBorder = new LineBorder(
				(Color) UIManager.get("Panel.alt.foreground"), 3);
		setBorder(unfocusBorder);

		addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				log.trace(getName() + " gained focus");
				setBorder(focusBorder);
			}

			/**
			 * Invoked when a component loses the keyboard focus.
			 */
			public void focusLost(FocusEvent e) {
				log.trace(getName() + " lost focus");
				setBorder(unfocusBorder);
			}

		});

		setEditable(false);

		// Create and add the main document style
		Style defaultStyle = sc.getStyle(StyleContext.DEFAULT_STYLE);

		int fontSize = (Integer) defaultStyle
				.getAttribute(StyleConstants.FontSize);
		// String fontFam = (String) defaultStyle
		// .getAttribute(StyleConstants.FontFamily);
		// boolean fontBold = (Boolean) defaultStyle
		// .getAttribute(StyleConstants.Bold);
		// Create and add the heading style
		heading2Style = sc.addStyle("Heading2", null);
		// StyleConstants.setFontSize(heading2Style, fontSize);
		// StyleConstants.setFontFamily(heading2Style, fontFam);
		// StyleConstants.setBold(heading2Style, fontBold);
		// StyleConstants.setForeground(heading2Style, Color.white);
		// StyleConstants.setBackground(heading2Style, Color.black);
		StyleConstants.setRightIndent(heading2Style, fontSize);
		StyleConstants.setLeftIndent(heading2Style, fontSize * 2);
		StyleConstants.setFirstLineIndent(heading2Style, fontSize * -2);

		speechStyle = sc.addStyle("SpeechStyle", defaultStyle);
		// StyleConstants.setForeground(speechStyle, Color.black);
		// StyleConstants.setBackground(speechStyle, Color.white);
		// StyleConstants.setBold(speechStyle, fontBold);
		// StyleConstants.setFontFamily(speechStyle, fontFam);
		// StyleConstants.setFontSize(speechStyle, fontSize);
		StyleConstants.setRightIndent(speechStyle, fontSize);
		StyleConstants.setLeftIndent(speechStyle, fontSize * 4);
		StyleConstants.setFirstLineIndent(speechStyle, fontSize * -2);

		simpleStyle = sc.addStyle("SimpleStyle", defaultStyle);
		StyleConstants.setRightIndent(simpleStyle, fontSize);
		StyleConstants.setLeftIndent(simpleStyle, fontSize);
		StyleConstants.setFirstLineIndent(simpleStyle, 0);
		// StyleConstants.setBold(simpleStyle, fontBold);
		// StyleConstants.setFontFamily(simpleStyle, fontFam);
		// StyleConstants.setFontSize(simpleStyle, fontSize);

	}

	/**
	 * Add Basic text String as in user's name
	 * 
	 * @param text
	 */
	public void addLine(String text) {
		append(text);
	}

	public void append(String s) {
		append(simpleStyle, getForeground(), getBackground(), s, true);
	}

	public void append(Color fgc, String s) {
		append(simpleStyle, fgc, getBackground(), s, true);
	}

	public boolean append(Style baseStyle, Color fgc, Color bgc, String msg,
			boolean addNL) {

		if (bottom > getHeight()) {
			log.warn("\n\n\n====== Previous scroll FAILED ============\n\n\n");

		}
		StringBuilder msgSb = new StringBuilder(new Date().toString());
		msgSb.append(": ").append(msg);
		Style textStyle = null;
		if (baseStyle != null) {
			if (bgc == null) {
				bgc = getBackground();
			}
			if (fgc == null) {
				fgc = getForeground();
			}
			StringBuilder sbKey = new StringBuilder(baseStyle.getName());
			sbKey.append(fgc.getRed()).append(fgc.getGreen())
					.append(fgc.getBlue());
			sbKey.append(bgc.getRed()).append(bgc.getGreen())
					.append(bgc.getBlue());

			// check to see if we already have a style that matches this
			// format/color set and if not make one.
			textStyle = sc.getStyle(sbKey.toString());
			if (textStyle == null) {
				textStyle = sc.addStyle(sbKey.toString(), baseStyle);
				StyleConstants.setForeground(textStyle, fgc);
				StyleConstants.setBackground(textStyle, bgc);
				Font f = (Font) UIManager.get("TextPane.font");
				StyleConstants.setBold(textStyle, f.isBold());
				StyleConstants.setFontFamily(textStyle, f.getFamily());
				StyleConstants.setFontSize(textStyle, f.getSize());
				log.info("Created:" + sbKey.toString() + "-" + f.toString());

			}
		}
		if (addNL) {
			msgSb.append('\n');
		}
		StyledDocument doc = getStyledDocument();
		int start = doc.getLength();
		if (start < docLen)
			start = docLen;
		try {
			doc.insertString(start, msgSb.toString(), textStyle);
			docLen = docLen + msgSb.length();
			if (textStyle != null) {
				doc.setParagraphAttributes(start, msgSb.length(), textStyle,
						true);
			}
			setCaretPosition(getDocument().getLength());
			if (log.isInfoEnabled()) {
				StringBuilder sb = new StringBuilder("Wrote:");
				sb.append(msgSb).append(" ").append(getName()).append("@")
						.append(start).append("/").append(getHeight() - 2)
						.append(" with attrs:");
				if (textStyle != null) {
					Enumeration<?> attrs = textStyle.getAttributeNames();
					while (attrs.hasMoreElements()) {
						Object key = attrs.nextElement();
						sb.append(key.toString()).append("=")
								.append(textStyle.getAttribute(key).toString())
								.append("\n");
					}
				}
				log.trace(sb.toString());
			}
		} catch (BadLocationException e) {
			log.error(e.getMessage() + " inserting " + msgSb.toString() + " @ "
					+ start + " of " + doc.getLength(), e);
		}
		lines++;
		// len = doc.getLength(); // same value as
		// setCaretPosition(len); // place caret at the beginning of line we
		// just
		// // added so screen reader will see it.
		// appDataPanel.getAppLogger().logInfo("caret was:" + len + " shows as:"
		// + getCaretPosition()
		// + " doc len:" + getDocument().getLength());
		// this.getRootPane().getAccessibleContext().firePropertyChange(
		// AccessibleContext.ACCESSIBLE_TEXT_PROPERTY, "", s);
		// getAccessibleContext().setAccessibleDescription(s);
		boolean rtn = true;
		// scrollBottom(level, doc);
		// scrollRectToVisible(new Rectangle(0,getHeight()-2,1,1));

		// requestFocusInWindow(true); //see CR-121

		return rtn;
	}

}
