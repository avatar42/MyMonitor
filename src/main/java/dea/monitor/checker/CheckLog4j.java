package dea.monitor.checker;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.apache.commons.io.input.TailerListenerAdapter;

/**
 * Read log4j log files looking for error and recovery messages
 * 
 * @author dea
 */
public class CheckLog4j extends CheckBase implements MultiCheckI<CheckLogItem> {
	// TODO: add check no update to log in over X minutes.
	private static final SimpleDateFormat logDateFormat = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");

	private String region = "home";
	private String logFile;
	private long delay;
	private HashMap<String, CheckLogItem> checks = new HashMap<String, CheckLogItem>();
	private boolean startAtEndOfLog = true;

	public CheckLog4j() {
		mutliCheck = true;
	}

	public void loadBundle() {
		region = getBundleVal(String.class, "region", region);
		logFile = getBundleVal(String.class, "log", null);
		delay = getBundleVal(Long.class, "delay", 100L);
		startAtEndOfLog = getBundleVal(Boolean.class, "startAtEndOfLog", true);
		String tests = getBundleVal(String.class, "tests", null);
		if (tests != null) {
			StringTokenizer st = new StringTokenizer(tests, ",");
			while (st.hasMoreTokens()) {
				String name = st.nextToken();
				String description = getBundleVal(String.class, name
						+ ".description", name);
				String errStr = getBundleVal(String.class, name + ".error",
						null);
				String okStr = getBundleVal(String.class, name + ".ok", null);
				CheckLogItem item = new CheckLogItem(name, description, region,
						errStr, okStr, getBundleVal(String.class, name
								+ ".errMsg", null), getBundleVal(String.class,
								name + ".okMsg", null));
				checks.put(name, item);
			}
		}
	}

	public void run() {

	}

	public Thread background() {
		TailerListener listener = new MyTailerListener(checks);
		Tailer tailer = new Tailer(new File(logFile), listener, delay,
				startAtEndOfLog);
		thread = new Thread(tailer);
		// thread.setDaemon(true); // optional
		thread.start();
		log.info("Tailer started for :" + logFile);
		return thread;
	}

	public boolean isStartAtEndOfLog() {
		return startAtEndOfLog;
	}

	public void setStartAtEndOfLog(boolean startAtEndOfLog) {
		this.startAtEndOfLog = startAtEndOfLog;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public long getDelay() {
		return delay;
	}

	public void setDelay(long delay) {
		this.delay = delay;
	}

	public HashMap<String, CheckLogItem> getChecks() {
		return checks;
	}

	public void setChecks(HashMap<String, CheckLogItem> checks) {
		this.checks = checks;
	}

	/**
	 * for testing
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		CheckLog4j item = new CheckLog4j();
		item.setStartAtEndOfLog(false);
		item.loadBundle("TCP");
		Thread thread = item.background();

		while (thread.isAlive()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		item.log.info("done:" + item.getErrStr());
		item.log.info("end:" + item.toString());

	}

	public class MyTailerListener extends TailerListenerAdapter {
		private HashMap<String, CheckLogItem> checks;

		public MyTailerListener(HashMap<String, CheckLogItem> checks) {
			super();
			this.checks = checks;
		}

		public Date getDateFromLine(String line) {
			Date d = null;
			int idx = line.indexOf(',');
			try {
				d = logDateFormat.parse(line.substring(0, idx));
			} catch (Exception e) {
				log.debug("Failed to get date from line:" + line);
				d = new Date();
			}

			return d;
		}

		public void handle(String line) {
			log.info("Read:" + line);
			Date d = getDateFromLine(line);
			for (CheckLogItem item : checks.values()) {
				if (item.getErrPattern() != null
						&& item.getErrPattern().matcher(line).find()) {
					if (item.getErrMsg() == null)
						item.setErrStr(d.toString() + ":" + item.getErrMsg(), d);
					else
						item.setErrStr(line, d);
					item.addDetails(line);

				} else if (item.getOkPattern() != null
						&& item.getOkPattern().matcher(line).find()) {
					item.setLastOK(d);
					StringBuilder sb = new StringBuilder();
					sb.append(d.toString()).append(":");
					if (item.getOkMsg() == null) {
						sb.append(line);
					} else {
						sb.append(item.getOkMsg());
					}
					sb.append(" ").append(item.getErrStr());
					item.addDetails(sb.toString());
					item.setErrStr(null, d);
				}
			}
		}
	}

}
