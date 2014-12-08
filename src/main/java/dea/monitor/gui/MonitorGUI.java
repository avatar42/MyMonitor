package dea.monitor.gui;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dea.monitor.checker.CheckItemI;
import dea.monitor.checker.ChildCheckItemI;
import dea.monitor.checker.MultiCheckI;

public class MonitorGUI {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private JFrame frame;
	private List<CheckItemI> checks;
	private Set<String> regions = new HashSet<String>();
	private ScrollingLogPane statusLog;
	private Map<String, JPanel> rPanels = new HashMap<String, JPanel>();
	private Map<String, CheckButton> buttons = new HashMap<String, CheckButton>();
	private JLabel status = new JLabel("Server Monitor");

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

		mainPanel.add(status, BorderLayout.NORTH);

		JPanel pane = new JPanel();
		pane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
		pane.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		// natural height, maximum width
		c.fill = GridBagConstraints.HORIZONTAL + GridBagConstraints.VERTICAL;

		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;

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
			pane.add(rpane, c);
			rPanels.put(region, rpane);
			c.gridx++;
		}

		for (CheckItemI item : checks) {
			JPanel rpane = rPanels.get(item.getRegion());
			CheckButton b = new CheckButton(item, statusLog, rpane);
			rpane.add(b);
			buttons.put(item.getName(), b);
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
