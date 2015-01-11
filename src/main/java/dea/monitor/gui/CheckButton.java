package dea.monitor.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FocusTraversalPolicy;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.UIManager;

import dea.monitor.checker.CheckItemI;

/**
 * Button to show status by color, shows status details in a dialog when
 * clicked. TODO: add right click does retry.
 * 
 * @author dea
 * 
 */
public class CheckButton extends JButton implements MouseListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final int STATE_UNKOWN = 0;
	public static final int STATE_OK = 1;
	public static final int STATE_ERR = 2;
	public static final int STATE_OK_WITH_ERR = 3;

	public static final int NULL_CHAR = KeyEvent.VK_UNDEFINED;
	public static final int FULL_MASK = 127;
	public static final int CHAR_MASK = 31;

	private CheckItemI item;
	private ScrollingLogPane statusLog;
	private String lastErr;
	private int state = STATE_UNKOWN;
	boolean mouseIn = false;
	float multiplier = 1;
	private boolean defaultBtn = false;
	private boolean navButton = false;
	private MouseListener mouseListener = null;
	// currently round or null (plain unmodified) are only options.
	// static so changes will be reflected in next repaint in all buttons.
	private static String type = null;
	private String helpText = null;
	// TODO: add help
	private String helpKey = null;
	private String helpTitle = null;
	private JPanel parent = null;
	private ErrDialog errDialog = null;

	public CheckButton(CheckItemI item, ScrollingLogPane statusLog,
			JPanel parent) {
		this(item, 1.0f, NULL_CHAR, false, statusLog, false, parent);
		// setText(item.getName());
		// setToolTipText(item.getDecription());
		// setVisible(true);

	}

	public ScrollingLogPane getStatusLog() {
		return statusLog;
	}

	public JPanel getParent() {
		return parent;
	}

	public String getLastErr() {
		return lastErr;
	}

	public void setLastErr(String lastErr) {
		this.lastErr = lastErr;
	}

	public int getState() {
		return state;
	}

	public void setState(int newState) {
		if (newState == STATE_OK && lastErr != null) {
			state = STATE_OK_WITH_ERR;
		} else {
			state = newState;
		}

		switch (state) {
		case STATE_OK:
			setBackground(Color.GREEN);
			break;
		case STATE_OK_WITH_ERR:
			setBackground(Color.YELLOW);
			break;
		case STATE_ERR:
			setBackground(Color.RED);
			break;
		default:
			setBackground(Color.WHITE);
		}
		setOpaque(true);
	}

	/**
	 * Base constructor all the other helper constructors call. Also used to
	 * construct help buttons.
	 * 
	 * @param label
	 * @param tip
	 * @param newMultiplier
	 * @param shortCut
	 * @param navButton
	 * @param statusLog
	 * @param isHelp
	 * @param parent
	 */
	public CheckButton(CheckItemI item, float newMultiplier,
			final int shortCut, boolean navButton,
			final ScrollingLogPane statusLog, boolean isHelp,
			final JPanel parent) {

		super(item.getName());
		this.item = item;
		this.statusLog = statusLog;
		this.navButton = navButton;
		this.multiplier = newMultiplier;
		this.parent = parent;
		helpText = item.getDescription();
		helpTitle = "Help for " + item.getName();
		// + Messages.getString("LoginDialog.button");
		type = (String) UIManager.get("Button.type");
		if (type != null && type.trim().length() == 0) {
			type = null;
		}

		// System.out.println("type=("+type+")");
		// text = label;

		if (type != null) {
			setBorderPainted(false);
			setContentAreaFilled(false);
		} else {
			setBorderPainted(true);
			setContentAreaFilled(true);
		}
		addMouseListener(this);
		if (shortCut != NULL_CHAR) {
			setMnemonic(shortCut);
			statusLog.logIt(ScrollingLogPane.SHOW_DEBUG, "\nSet shortcut to "
					+ (char) shortCut + " for " + item.getName() + "\n");
			helpText = helpText + "<br>Shortcut for this button is Alt+"
					+ (char) shortCut;
		}
		helpText = helpText
				+ "<br>In keyboard navigation mode this button can be selected by pressing the Enter or Space keys";
		setName(item.getName());
		setToolTipText(item.getDescription());
		getAccessibleContext().setAccessibleName(item.getName());// +
																	// " Button");
		getAccessibleContext().setAccessibleDescription(item.getDescription());

		setOpaque(true);
		int w = 0;
		if (navButton) {
			String justify = (String) UIManager.get("Button.text.justify");
			if (justify == null || justify.trim().length() == 0) {
				setAlignmentY(Component.CENTER_ALIGNMENT);
			} else if ("left".equals(justify)) {
				setAlignmentY(Component.LEFT_ALIGNMENT);
			} else if ("right".equals(justify)) {
				setAlignmentY(Component.RIGHT_ALIGNMENT);
			} else {
				setAlignmentY(Component.CENTER_ALIGNMENT);
			}
			statusLog.logIt(ScrollingLogPane.SHOW_DEBUG,
					"Set CheckButton justify:" + justify);
			String width = (String) UIManager.get("Button.size");
			if (width != null && width.trim().length() > 0) {
				try {
					w = Integer.parseInt(width);
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			}

			setMargin(new Insets(0, 0, 0, 0));
		} else {
			setAlignmentY(Component.CENTER_ALIGNMENT);
		}
		setAlignmentX(Component.CENTER_ALIGNMENT);

		Dimension preferredSize = getPreferredSize();
		if (w > 0) {
			setSize(w, getHeight());
			statusLog.logIt(ScrollingLogPane.SHOW_DEBUG,
					"Set CheckButton size:" + w);
		}

		// float helpMultiplier = 1.0f;
		if (isHelp) {
			// helpMultiplier = 1.9f;
			this.setMargin(new Insets(1, 1, 1, 1));
		} else {
			// add link to helpDialog
			KeyAdapter ka = new KeyAdapter() {
				public void keyReleased(KeyEvent e) {
					// Key pressed:?(ffffh)65535 mod:0 code:112
					char c = e.getKeyChar();
					int mod = e.getModifiers();
					int code = e.getKeyCode();
					statusLog.logIt(ScrollingLogPane.SHOW_DEBUG, "Key pressed:"
							+ c + "(" + Integer.toHexString(c) + "h)" + (int) c
							+ " mod:" + mod + " code:" + code + ") in "
							+ e.getSource().toString());
					if (code == KeyEvent.VK_F1 && mod == 0) {
						if (mouseListener != null) {
							statusLog.logIt(ScrollingLogPane.SHOW_DEBUG,
									"Showing help for button " + getName());
							// TODO: add help
							// statusLog.showHelp(helpKey, parent, helpText,
							// helpTitle);
						}
					}
				}
			};
			addKeyListener(ka);
		}

		Font f = (Font) UIManager.get("Button.font");
		Font nf = new Font(f.getFontName(), f.getStyle(),
				(int) (f.getSize() * multiplier));
		super.setFont(nf);

		FontMetrics fm = getFontMetrics(getFont());
		// String t = getText();
		statusLog.logIt(ScrollingLogPane.SHOW_DEBUG,
				"getText():" + item.getName());
		int x = fm.stringWidth(item.getName());
		statusLog.logIt(ScrollingLogPane.SHOW_DEBUG, "x:" + x
				+ " preferredSize.width:" + preferredSize.width);
		if (x < preferredSize.width && !isHelp)
			x = preferredSize.width;
		Dimension nd = new Dimension((int) (x * multiplier),
				(int) (preferredSize.height * multiplier));

		statusLog.logIt(ScrollingLogPane.SHOW_DEBUG, "Setting size to:"
				+ nd.width + "X" + nd.height);
		setMinimumSize(nd);
		statusLog.logIt(ScrollingLogPane.SHOW_DEBUG, getName() + " Button now:"
				+ toString());

		if (mouseListener != null) {
			addMouseListener(mouseListener);
		}

		final CheckButton me = this;
		KeyAdapter ka = new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				if (!me.isEnabled()) {
					return;
				}
				char c = e.getKeyChar();
				int mod = e.getModifiers();
				int code = e.getKeyCode();
				statusLog.logIt(ScrollingLogPane.SHOW_DEBUG, "Key pressed:" + c
						+ "(" + Integer.toHexString(c) + "h)" + (int) c
						+ " mod:" + mod + " code:" + code
						+ " looking for shortCut (" + shortCut + ") in "
						+ e.getSource().toString());
				if (c <= 'z') {

					statusLog
							.logIt(ScrollingLogPane.SHOW_DEBUG,
									"\n"
											+ Integer.toBinaryString(c)
											+ ":char\n"
											+ Integer.toBinaryString(shortCut)
											+ ":shortcut\n"
											+ Integer.toBinaryString(FULL_MASK)
											+ "\n"
											+ Integer
													.toBinaryString((c & CHAR_MASK) + 64)
											+ ":char\n"
											+ Integer
													.toBinaryString((shortCut & CHAR_MASK) + 64)
											+ ":shortcut\n");
					// check upper and lower case and Alt+
					// if (shortCut != NULL_CHAR && ((int) c & MASK) == shortCut
					// || ((((int) c & CHAR_MASK) == shortCut) && mod == 8)) {
					if (shortCut != NULL_CHAR
							&& ((int) c & CHAR_MASK) == (shortCut & CHAR_MASK)
							&& ((mod & 8) == 8)) {
						if (mouseListener != null) {
							statusLog.logIt(
									ScrollingLogPane.SHOW_DEBUG,
									"Converting shortCut to click:"
											+ me.getName());
							mouseListener.mousePressed(null);
						}
					} else {

						statusLog
								.logIt(ScrollingLogPane.SHOW_DEBUG,
										"defaultBtn:" + defaultBtn
												+ " hasFocus():"
												+ me.hasFocus() + " mouseIn:"
												+ mouseIn);
						if ((defaultBtn || me.hasFocus() || mouseIn)
								&& (c == KeyEvent.VK_ENTER || c == KeyEvent.VK_SPACE)) {
							if (mouseListener != null) {
								statusLog.logIt(ScrollingLogPane.SHOW_DEBUG,
										"Converting enter / space to click:"
												+ me.getName());
								mouseListener.mousePressed(null);
							}
						}
					}
				} else {
					Component nextComp = null;
					Container container = getFocusCycleRootAncestor();
					FocusTraversalPolicy policy = container
							.getFocusTraversalPolicy();
					if (code == 37 || code == 38) { // left or up
						nextComp = policy.getComponentBefore(container, me);
					} else if (code == 39 || code == 40) { // right or down
						nextComp = policy.getComponentAfter(container, me);
					}
					if (nextComp != null) {
						nextComp.requestFocusInWindow();
					}
				}
			}

		};

		addKeyListener(ka);
		setFocusTraversalKeysEnabled(true);

		addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				statusLog.logIt(ScrollingLogPane.SHOW_DEBUG, getText()
						+ " should gain focus");
				if (isEnabled()) {
					statusLog.logIt(ScrollingLogPane.SHOW_DEBUG, getText()
							+ " gained focus");
					mouseIn = true;
					forcePaint();
				}
			}

			/**
			 * Invoked when a component loses the keyboard focus.
			 */
			public void focusLost(FocusEvent e) {
				if (isEnabled()) {
					statusLog.logIt(ScrollingLogPane.SHOW_DEBUG, getText()
							+ " lost focus");
					mouseIn = false;

					forcePaint();
				}
			}

		});
	}

	public boolean isMouseIn() {
		return mouseIn;
	}

	public void setMouseIn(boolean mouseIn) {
		this.mouseIn = mouseIn;
	}

	public boolean isNavButton() {
		return navButton;
	}

	public void setNavButton(boolean navButton) {
		this.navButton = navButton;
	}

	/**
	 * Keep track of MouseListener added that is not this object so we can fake
	 * click on key press
	 */
	public void addMouseListener2(MouseListener l) {
		if (l == null) {
			return;
		}
		addMouseListener(l);
		if (l == this) {
			return;
		}
		statusLog.logIt(ScrollingLogPane.SHOW_DEBUG,
				getText() + " adding:" + l.toString());
		mouseListener = l;
	}

	private boolean isTreeVisible(Component c) {
		boolean rtn = false;
		if (c.isVisible()) {
			Component p = c.getParent();
			// if parent not null and this is not a Frame or Dialog then check
			// its parent
			if (p != null && c instanceof Component && !(c instanceof Window)) {
				rtn = isTreeVisible(c.getParent());
			} else {
				rtn = true;
			}
		} else {
			rtn = false;
		}

		statusLog.logIt(ScrollingLogPane.SHOW_TRACE, c.getClass().getName()
				+ ":" + c.getName() + " isVisible()" + c.isVisible());

		return rtn;
	}

	/**
	 * force redraw of button if still visible
	 */
	public void forcePaint() {
		if (isTreeVisible(this)) {
			paintComponent(getGraphics());
		}
	}

	@Override
	public void paintComponent(Graphics g) {
		statusLog.logIt(ScrollingLogPane.SHOW_DEBUG, getText() + ".type="
				+ type + " nav=" + isNavButton() + " Using font "
				+ getFont().toString() + " bounds=" + getBounds().width);
		if (type == null) {
			paintDefault(g);
		} else if ("round".equals(type)) {
			paintOval(g);
		} else if ("Lotus".equals(type)) {
			if (isNavButton()) {
				paintLotusNav(g);
			} else {
				paintLotus(g);
			}
		} else {
			paintDefault(g);
		}
	}

	public void paintDefault(Graphics g) {
		setBorderPainted(true);
		setContentAreaFilled(true);
		revalidate();
		super.paintComponent(g);
	}

	public void paintLotus(Graphics g) {
		setBorderPainted(false);
		setContentAreaFilled(false);
		Color cHigh = (Color) UIManager.get("Button.highlight");
		Color cNormBG = (Color) UIManager.get("Button.background");
		double radius = getHeight();// * 0.92;
		statusLog.logIt(ScrollingLogPane.SHOW_TRACE, "h=" + getHeight() + " w="
				+ getWidth() + "r=" + radius);
		Graphics2D g2 = (Graphics2D) g;

		if (getModel().isPressed()) {
			g.setColor(g.getColor());
			g2.fillRect(3, 3, getWidth() - 6, getHeight() - 6);
		}
		try {
			super.paintComponent(g);
		} catch (Exception e) {
			isTreeVisible(this);
			statusLog.logErr(getText()
					+ ":paintLotus(Graphics g).super.paintComponent(g)", e);
		}

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_OFF);

		if (mouseIn && isEnabled())
			g2.setColor(cHigh);
		else
			g2.setColor(cNormBG);
		g2.fillRect(1, 1, (getWidth() - 3), (getHeight() - 3));

		g2.setColor((Color) UIManager.get("Button.foreground"));

		Color borderColor = (Color) UIManager.get("Button.borderColor");
		if (borderColor != null) {
			g2.setColor(borderColor);
		}
		// draw the border
		g2.setStroke(new BasicStroke(1.0f * multiplier));
		g2.draw(new Rectangle2D.Double(1, 1, (getWidth() - 3),
				(getHeight() - 3)));

		// draw the label centered in the button
		Font f = getFont();
		if (f != null) {
			FontMetrics fm = getFontMetrics(getFont());
			if (multiplier > 1) {
				statusLog.logIt(ScrollingLogPane.SHOW_DEBUG, "Using font "
						+ g.getFont().toString());
			}
			g.setColor(getForeground());
			int x = getWidth() / 2 - fm.stringWidth(getText()) / 2;
			if (getAlignmentY() == Component.LEFT_ALIGNMENT) {
				x = 1;
			} else if (getAlignmentY() == Component.RIGHT_ALIGNMENT) {
				x = getWidth() - fm.stringWidth(getText());
			}
			int y = getHeight() / 2 + fm.getMaxDescent();
			g.drawString(getText(), x, y);
			if (getDisplayedMnemonicIndex() > -1) {
				int offset = fm.stringWidth(getText().substring(0,
						getDisplayedMnemonicIndex()));
				g.drawLine(x + offset, y + 1,
						x + offset + fm.charWidth(getMnemonic()), y + 1);
			}
		}
		g2.dispose();
	}

	public void paintLotusNav(Graphics g) {
		setBorderPainted(false);
		setContentAreaFilled(false);
		Color cHigh = (Color) UIManager.get("Button.highlight");
		Color cNormBG = (Color) UIManager.get("Button.focus");
		double radius = getHeight();// * 0.92;
		statusLog.logIt(ScrollingLogPane.SHOW_TRACE, "h=" + getHeight() + " w="
				+ getWidth() + "r=" + radius);
		Graphics2D g2 = (Graphics2D) g;

		if (getModel().isPressed()) {
			g.setColor(g.getColor());
			g2.fillRect(3, 3, getWidth() - 6, getHeight() - 6);
		}
		super.paintComponent(g);

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_OFF);

		if (mouseIn && isEnabled())
			g2.setColor(cHigh);
		else
			g2.setColor(cNormBG);
		g2.fillRect(1, 1, (getWidth() - 3), (getHeight() - 3));

		// Color borderColor = (Color) UIManager.get("Button.borderColor");
		// if (borderColor != null) {
		// g2.setColor(borderColor);
		// }
		// draw the border
		g2.setStroke(new BasicStroke(2.0f * multiplier));
		g2.draw(new Rectangle2D.Double(1, 1, (getWidth() - 3),
				(getHeight() - 3)));

		// draw the label centered in the button
		Font f = getFont();
		if (f != null) {
			FontMetrics fm = getFontMetrics(getFont());
			if (multiplier > 1) {
				statusLog.logIt(ScrollingLogPane.SHOW_DEBUG, "Using font "
						+ g.getFont().toString());
			}
			g.setColor(getForeground());
			int x = getWidth() / 2 - fm.stringWidth(getText()) / 2;
			if (getAlignmentY() == Component.LEFT_ALIGNMENT) {
				x = 1;
			} else if (getAlignmentY() == Component.RIGHT_ALIGNMENT) {
				x = getWidth() - fm.stringWidth(getText());
			}
			int y = getHeight() / 2 + fm.getMaxDescent();
			g.drawString(getText(), x, y);
			if (getDisplayedMnemonicIndex() > -1) {
				int offset = fm.stringWidth(getText().substring(0,
						getDisplayedMnemonicIndex()));
				g.drawLine(x + offset, y + 1,
						x + offset + fm.charWidth(getMnemonic()), y + 1);
			}
		}
		g2.dispose();
	}

	public void paintOval(Graphics g) {
		setBorderPainted(false);
		setContentAreaFilled(true);
		Color cHigh = (Color) UIManager.get("Button.highlight");
		Color cFocus = (Color) UIManager.get("Button.focus");
		double radius = getHeight() * 0.92;
		statusLog.logIt(ScrollingLogPane.SHOW_TRACE, "h=" + getHeight() + " w="
				+ getWidth() + "r=" + radius);
		Graphics2D g2 = (Graphics2D) g;

		if (getModel().isPressed()) {
			g.setColor(g.getColor());
			g2.fillRect(3, 3, getWidth() - 6, getHeight() - 6);
		}
		super.paintComponent(g);

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_OFF);

		Color borderColor = (Color) UIManager.get("Button.borderColor");
		if (mouseIn && isEnabled()) {
			g2.setColor(cHigh);
			if (borderColor == null) {
				borderColor = cHigh;
			}
		} else {
			g2.setColor(cFocus);
			borderColor = cFocus;
		}
		g2.fillRoundRect(1, 1, (getWidth() - 3), (getHeight() - 3),
				(int) radius, (int) radius);

		g2.setColor(borderColor);
		// draw the rounded border
		g2.setStroke(new BasicStroke(2.0f * multiplier));
		g2.draw(new RoundRectangle2D.Double(1, 1, (getWidth() - 3),
				(getHeight() - 3), radius, radius));

		// draw the label centered in the button
		Font f = getFont();
		if (f != null) {
			FontMetrics fm = getFontMetrics(getFont());
			if (multiplier > 1) {
				statusLog.logIt(ScrollingLogPane.SHOW_DEBUG, "Using font "
						+ g.getFont().toString());
			}
			g.setColor(getForeground());
			int x = getWidth() / 2 - fm.stringWidth(getText()) / 2;
			int y = getHeight() / 2 + fm.getMaxDescent();
			g.drawString(getText(), x, y);
			if (getDisplayedMnemonicIndex() > -1) {
				int offset = fm.stringWidth(getText().substring(0,
						getDisplayedMnemonicIndex()));
				g.drawLine(x + offset, y + 1,
						x + offset + fm.charWidth(getMnemonic()), y + 1);
			}
		}
		g2.dispose();
	}

	/**
	 * Enables (or disables) the button.
	 * 
	 * @param b
	 *            true to enable the button, otherwise false
	 */
	public void setEnabled(boolean b) {
		super.setEnabled(b);
		super.setFocusable(b);
		statusLog.logIt(ScrollingLogPane.SHOW_TRACE, "Button " + getText()
				+ " setting enabled to " + b);
		Color foreground = (Color) UIManager.get("Button.foreground");
		if (b) {
			setForeground(foreground);
		} else {
			setForeground(Colors.getInverse(foreground));
			// setForeground((Color)
			// UIManager.get("Button.disabledForeground"));
		}
	}

	// "Cancel", "Retry", "Clear Error"
	private void showError() {
		if (errDialog == null) {
			errDialog = new ErrDialog(statusLog.getFrame(), this);
		} else {
			errDialog.update();
		}
	}

	public void mouseClicked(MouseEvent e) {
		if (e == null || !e.isMetaDown()) {
			statusLog.logIt(ScrollingLogPane.SHOW_DEBUG, getName()
					+ "Internal MouseListener has ignored click");
			showError();
		}
	}

	public void mouseEntered(MouseEvent e) {
		mouseIn = true;
	}

	public void mouseExited(MouseEvent e) {
		mouseIn = false;
	}

	public void mousePressed(MouseEvent e) {
		statusLog.logIt(ScrollingLogPane.SHOW_INFO,
				"Internal MouseListener has ignored press");
		if (e.isMetaDown()) {
			// TODO: add help
			// statusLog.showHelp(helpKey, parent, helpText, helpTitle);
		} else {
			Color foreground = (Color) UIManager.get("Button.foreground");
			if (isEnabled()) {
				// setForeground(Colors.getInverse(foreground));
				showError();
			}
		}
	}

	public void mouseReleased(MouseEvent e) {
		statusLog.logIt(ScrollingLogPane.SHOW_DEBUG, getName()
				+ "Internal MouseListener has ignored release");
		Color foreground = (Color) UIManager.get("Button.foreground");
		if (isEnabled()) {
			setForeground(foreground);
		}
	}

	public void setFont(Font f) {
		Font nf = new Font(f.getFontName(), f.getStyle(),
				(int) (f.getSize() * multiplier));
		super.setFont(nf);
	}

	public boolean requestFocusInWindow() {
		boolean rtn = super.requestFocusInWindow();
		statusLog.logIt(ScrollingLogPane.SHOW_WARNINGS, getName()
				+ ".requestFocusInWindow()=" + rtn);
		return rtn;
	}

	public void requestFocus() {
		super.requestFocus();
		statusLog.logIt(ScrollingLogPane.SHOW_WARNINGS, getName()
				+ ".requestFocus()");
	}

	public float getMultiplier() {
		return multiplier;
	}

	public void setMultiplier(float multiplier) {
		this.multiplier = multiplier;
	}

	public CheckItemI getItem() {
		return item;
	}

}
