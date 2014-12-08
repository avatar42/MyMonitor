package dea.monitor.gui;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScrollingLogPane extends JScrollPane {
	public static final int SHOW_ERRORS = 0;
	public static final int SHOW_WARNINGS = 1;
	public static final int SHOW_INFO = 2;
	public static final int SHOW_DEBUG = 3;
	public static final int SHOW_TRACE = 4;

	private int logLevel = SHOW_INFO;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private StatusPane statusLog;
	// holds a pointer to the top level frame
	private JFrame frame;

	public ScrollingLogPane(int width, int logHeight, JFrame frame) {
		this.frame = frame;
		statusLog = new StatusPane();
		setViewportView(statusLog);

		Dimension minimumSize = new Dimension(width, logHeight);
		setMinimumSize(minimumSize);
		setPreferredSize(minimumSize);
	}

	public void logErr(String msg, Throwable t) {
		log.error(msg, t);
		statusLog.append(Color.RED, msg);
	}

	public void logIt(int level, String msg) {
		switch (level) {
		case SHOW_ERRORS:
			log.error(msg);
			break;
		case SHOW_WARNINGS:
			log.warn(msg);
			break;
		case SHOW_INFO:
			log.info(msg);
			break;
		case SHOW_DEBUG:
			log.debug(msg);
			break;
		default:
			log.trace(msg);
		}
		addLine(level, msg);
	}

	public void addLine(int level, String msg) {
		if (level <= logLevel) {

			switch (level) {
			case SHOW_ERRORS:
				statusLog.append(Color.RED, msg);
				break;
			case SHOW_WARNINGS:
				statusLog.append(Color.BLUE, msg);
				break;
			default:
				statusLog.addLine(msg);
			}
		}

	}

	public int getLogLevel() {
		return logLevel;
	}

	public void setLogLevel(int logLevel) {
		this.logLevel = logLevel;
	}

	public JFrame getFrame() {
		return frame;
	}

}
