package dea.monitor.gui;

import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dea.monitor.checker.CheckItemI;
import dea.monitor.checker.ChildCheckItemI;

public class RunChecks implements Runnable {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private boolean running = false;

	private List<CheckItemI> checks;
	private Map<String, CheckButton> buttons;
	private ScrollingLogPane statusLog;

	public RunChecks(ScrollingLogPane statusLog, List<CheckItemI> checks,
			Map<String, CheckButton> buttons) {
		super();
		this.checks = checks;
		this.buttons = buttons;
		this.statusLog = statusLog;
	}

	private void setButtonStatus(CheckItemI item) {
		if (item.getErrStr() != null) {
			try {
				statusLog.logIt(ScrollingLogPane.SHOW_ERRORS, item.getName()
						+ ":" + item.getErrStr());
			} catch (Exception e) {
				log.error("Failed writing to log pane:" + item.getName() + ":"
						+ item.getErrStr(), e);
			}
			CheckButton b = buttons.get(item.getName());
			b.setLastErr(item.getErrStr());
			b.setState(CheckButton.STATE_ERR);
			item.setErrStr(null);
		} else if (item.isLastRunOK()) {
			CheckButton b = buttons.get(item.getName());
			b.setState(CheckButton.STATE_OK);
		}

	}

	public void run() {

		running = true;
		while (running) {
			for (CheckItemI item : checks) {
				GregorianCalendar now = new GregorianCalendar();
				if (item instanceof ChildCheckItemI) {
					ChildCheckItemI child = (ChildCheckItemI) item;
					if (!child.isRead()) {
						setButtonStatus(child);
						child.setRead();
					}
				} else if (!item.isRunning()) {
					setButtonStatus(item);
					if (item.getNextRun().before(now)) {
						item.background();
					}
				} else if (item.getNextRun().before(now)) {
					CheckButton b = buttons.get(item.getName());
					b.setLastErr(item.getName() + ": appears hung");
					b.setState(CheckButton.STATE_ERR);
					statusLog.logIt(ScrollingLogPane.SHOW_ERRORS,
							item.getName() + ": appears hung");
					statusLog.logIt(ScrollingLogPane.SHOW_ERRORS,
							item.getName() + " killed: " + item.killThread());
					item.background();
				}
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

}
