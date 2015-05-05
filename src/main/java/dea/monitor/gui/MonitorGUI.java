package dea.monitor.gui;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dea.monitor.checker.CheckItemI;
import dea.monitor.checker.ChildCheckItemI;
import dea.monitor.checker.MultiCheckI;

/**
 * Main window / run class for monitor TODO: add menu option the retry all in
 * error checks.
 * 
 * @author dea
 * 
 */
public class MonitorGUI {
	public static final int RECHECK_ALL = 1;
	public static final int RECHECK_ERRORS = 2;
	public static final int CLEAR_ERRORS = 3;

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private JFrame frame;
	private List<CheckItemI> checks;
	private Set<String> regions = new HashSet<String>();
	private ScrollingLogPane statusLog;
	private Map<String, JPanel> rPanels = new HashMap<String, JPanel>();
	private Map<String, CheckButton> buttons = new HashMap<String, CheckButton>();
	private JMenuBar menuBar = new JMenuBar();
	private ResourceBundle bundle;

	@SuppressWarnings("unchecked")
	protected <T> T getBundleVal(Class<T> asClass, String key,
			Object defaultValue) {
		if (bundle.containsKey(key)) {
			try {
				if (Integer.class.isAssignableFrom(asClass))
					return (T) new Integer(bundle.getString(key));

				if (Boolean.class.isAssignableFrom(asClass))
					return (T) new Boolean(bundle.getString(key));

				if (String.class.isAssignableFrom(asClass))
					return (T) bundle.getString(key);

			} catch (Exception e) {
				log.error("Failed to parse " + key + ":"
						+ bundle.getString(key));
			}
		}
		log.warn("Using default value for " + key + ":" + defaultValue);
		return (T) defaultValue;
	}

	public MonitorGUI() {
		checks = new ArrayList<CheckItemI>();
		bundle = ResourceBundle.getBundle("checks");

		for (String key : bundle.keySet()) {
			if (key.startsWith("check.")) {
				String className = bundle.getString(key);
				try {
					Class<?> hiClass = Class.forName(className);
					CheckItemI instance = (CheckItemI) hiClass.newInstance();
					instance.loadBundle(key.substring(6));
					if (instance.isMutliCheck()) {
						for (ChildCheckItemI child : ((MultiCheckI<?>) instance)
								.getChecks().values()) {
							checks.add(child);
						}

					} else {
						checks.add(instance);
					}
					instance.background();
					regions.add(instance.getRegion());
				} catch (Exception e) {
					log.error("Failed to load:" + key, e);
				}
			} else if (key.startsWith("handler.")) {
				String className = bundle.getString(key);
				try {
					Class<?> hiClass = Class.forName(className);
					CheckItemI instance = (CheckItemI) hiClass.newInstance();
					instance.loadBundle(key.substring(8));
					checks.add(instance);
					regions.add(instance.getRegion());
				} catch (Exception e) {
					log.error("Failed to load:" + key, e);
				}
			}
		}
	}

	/**
	 * Create the GUI and show it. For thread safety, this method should be
	 * invoked from the event-dispatching thread.
	 */
	private void buildGUI(String[] args) {
		int minWidth = getBundleVal(Integer.class, "width.min", 800);
		int minHeight = getBundleVal(Integer.class, "height.min", 600);
		int logHeight = 100;
		parseArgs(args);
		frame = new JFrame("Server Monitor");
		frame.setVisible(true); // so icon shows in tray
		Dimension minimumSize = new Dimension(minWidth, minHeight);
		frame.setMinimumSize(minimumSize);
		frame.setPreferredSize(minimumSize);
		Dimension maxSize = new Dimension(getBundleVal(Integer.class,
				"width.max", 1920), getBundleVal(Integer.class, "height.max",
				1080));
		frame.setMaximumSize(maxSize);

		final JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.setOpaque(true);

		statusLog = new ScrollingLogPane(minWidth, logHeight, frame);
		mainPanel.add(statusLog, BorderLayout.SOUTH);
		statusLog.logIt(ScrollingLogPane.SHOW_INFO, "Initializing");

		// Build the first menu.
		JMenu menu = new JMenu("Reset");
		menu.setMnemonic(KeyEvent.VK_R);
		menu.getAccessibleContext().setAccessibleDescription(
				"Button reset options");
		menuBar.add(menu);

		// reset options
		JMenuItem menuItem = new JMenuItem(new ResetAction(RECHECK_ALL,
				"Recheck All"));
		menuItem.setMnemonic(KeyEvent.VK_A);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,
				ActionEvent.ALT_MASK));
		menuItem.getAccessibleContext().setAccessibleDescription(
				"Set all button to recheck now.");

		menu.add(menuItem);

		menuItem = new JMenuItem(new ResetAction(RECHECK_ERRORS,
				"Recheck Errors"));
		menuItem.setMnemonic(KeyEvent.VK_E);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E,
				ActionEvent.ALT_MASK));
		menuItem.getAccessibleContext()
				.setAccessibleDescription(
						"Recheck all buttons in error status and clear error details on buttons that have recovered from an error.");

		menu.add(menuItem);

		menuItem = new JMenuItem(new ResetAction(CLEAR_ERRORS, "Clear Errors"));
		menuItem.setMnemonic(KeyEvent.VK_C);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
				ActionEvent.ALT_MASK));
		menuItem.getAccessibleContext().setAccessibleDescription(
				"Remove error details from all buttons.");

		menu.add(menuItem);

		mainPanel.add(menuBar, BorderLayout.NORTH);

		JPanel pane = new JPanel();
		pane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
		pane.setLayout(new GridBagLayout());

		for (String region : regions) {
			JPanel rpane = new JPanel();
			rpane.getAccessibleContext().setAccessibleName(region);
			rpane.setBorder(ComponentHelper.getBorder(region));
			rpane.setVisible(true);
			rpane.setName(region);
			minimumSize = new Dimension(minWidth / regions.size(), minHeight
					- logHeight);
			rpane.setMinimumSize(minimumSize);
			rpane.setPreferredSize(minimumSize);
			rPanels.put(region, rpane);
		}

		for (CheckItemI item : checks) {
			JPanel rpane = rPanels.get(item.getRegion());
			CheckButton b = new CheckButton(item, statusLog, rpane);
			rpane.add(b);
			buttons.put(item.getName(), b);
		}

		GridBagConstraints c = new GridBagConstraints();
		// natural height, maximum width
		c.fill = GridBagConstraints.HORIZONTAL + GridBagConstraints.VERTICAL;

		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;

		for (JPanel rpane : rPanels.values()) {
			pane.add(rpane, c);
			c.gridx++;
		}

		mainPanel.add(pane, BorderLayout.CENTER);

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(mainPanel);

		// Display the window.
		SwingUtilities.updateComponentTreeUI(frame);
		frame.pack();
		frame.setLocationRelativeTo(null); // center on screen
		frame.setVisible(true);
		statusLog.logIt(ScrollingLogPane.SHOW_INFO, "GUI up");

		RunChecks rc = new RunChecks(statusLog, checks, buttons);
		Thread thread = new Thread(rc);
		thread.setName("RunChecks");
		thread.start();

		statusLog.logIt(ScrollingLogPane.SHOW_INFO, "Checks running");

	}

	/**
	 * Parse command line args
	 * 
	 * @param args
	 */
	public void parseArgs(String[] args) {
	}

	class ResetAction extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		int resetType;

		// This is our sample action. It must have an actionPerformed() method,
		// which is called when the action should be invoked.
		public ResetAction(int resetType, String text) {
			super(text, null);
			this.resetType = resetType;
		}

		public void actionPerformed(ActionEvent e) {
			for (CheckButton cb : buttons.values()) {
				if (resetType == RECHECK_ALL) { // retry all now
					cb.getItem().setNextRun(new GregorianCalendar());
					cb.setLastErr(null);
					cb.getItem().setDetails(null);
					cb.setState(CheckButton.STATE_UNKOWN);
				} else if (resetType == RECHECK_ERRORS) {
					if (cb.getState() == CheckButton.STATE_ERR) {
						cb.getItem().setNextRun(new GregorianCalendar());
						cb.setState(CheckButton.STATE_UNKOWN);
					}
					if (cb.getState() == CheckButton.STATE_OK_WITH_ERR) {
						cb.setState(CheckButton.STATE_ERR);
					}
					cb.setLastErr(null);
					cb.getItem().setDetails(null);
				} else if (resetType == CLEAR_ERRORS) {
					if (cb.getState() == CheckButton.STATE_ERR) {
						cb.setState(CheckButton.STATE_UNKOWN);
					}
					if (cb.getState() == CheckButton.STATE_OK_WITH_ERR) {
						cb.setState(CheckButton.STATE_ERR);
					}
					if (cb.getState() == CheckButton.STATE_ERR
							|| cb.getState() == CheckButton.STATE_OK_WITH_ERR) {
						cb.setLastErr(null);
						cb.getItem().setDetails(null);
					}
				}
			}
			System.out.println("Action [" + e.getActionCommand()
					+ "] performed!");
		}
	}

	public static void main(String[] args) {
		final String[] parms = args;
		// Schedule a job for the event-dispatching thread:
		// creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				MonitorGUI gui = new MonitorGUI();
				gui.buildGUI(parms);
			}
		});
	}

}
