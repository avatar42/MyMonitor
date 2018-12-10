package dea.monitor.checker;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestChecks implements Runnable {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private boolean running = false;

	private List<CheckItemI> checks;

	public void run() {
		checks = new ArrayList<CheckItemI>();
		ResourceBundle bundle = ResourceBundle.getBundle("checks");
		for (String key : bundle.keySet()) {
			String className = bundle.getString(key);
			try {
				Class<?> hiClass = Class.forName(className);
				CheckItemI instance = (CheckItemI) hiClass.newInstance();
				instance.loadBundle(key);
				checks.add(instance);
			} catch (Exception e) {
				log.error("Failed to load:" + key, e);
			}
		}

		running = true;
		while (running) {
			for (CheckItemI item : checks) {
				GregorianCalendar now = new GregorianCalendar();
				if (!item.isRunning()) {
					if (item.getErrStr() != null) {
						log.error(item.getName() + ":" + item.getErrStr());
						item.setErrStr(null);
					}
					if (item.getNextRun().before(now)) {
						now.add(Calendar.SECOND, item.getWait());
						item.setNextRun(now);
						item.background();
					}
				} else if (item.getNextRun().before(now)) {
					log.error(item.getName() + ": appears hung");
					log.error(item.getName() + " killed: " + item.killThread());
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

	/**
	 * for testing
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		TestChecks rc = new TestChecks();
		Thread thread = new Thread(rc);
		thread.setName("RunChecks");
		thread.start();

		while (thread.isAlive()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		rc.log.info("done");
	}

}
