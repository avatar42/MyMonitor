package dea.monitor.checker;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CheckBase implements CheckItemI {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private String name;
	private String description;
	private String region = "home";
	private int wait = 1800; // seconds between checks
	private GregorianCalendar nextRun = new GregorianCalendar();
	private Date lastOK;
	private boolean lastRunOK = false;
	private String errStr = null;
	private String details = null;
	protected ResourceBundle bundle;
	protected boolean running = false;
	protected Thread thread;
	protected String contentType = "text/text;";
	protected BufferedImage savedImg;
	protected boolean mutliCheck = false;

	protected abstract void loadBundle();

	public void loadBundle(String bundleName) {
		bundle = ResourceBundle.getBundle(bundleName);
		name = bundleName;
		wait = getBundleVal(Integer.class, "wait", wait);
		description = getBundleVal(String.class, "description", name);
		region = getBundleVal(String.class, "region", region);
		loadBundle();
	}

	/**
	 * Get value from bundle. Supported types are Integer, Long, Boolean or
	 * ArrayList<String>
	 * 
	 * @param asClass
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected <T> T getBundleVal(Class<T> asClass, String key, T defaultValue) {
		if (bundle.containsKey(key)) {
			try {
				if (Integer.class.isAssignableFrom(asClass))
					return (T) new Integer(bundle.getString(key));

				if (Long.class.isAssignableFrom(asClass))
					return (T) new Long(bundle.getString(key));

				if (Boolean.class.isAssignableFrom(asClass))
					return (T) new Boolean(bundle.getString(key));

				if (String.class.isAssignableFrom(asClass))
					return (T) bundle.getString(key);

				if (ArrayList.class.isAssignableFrom(asClass)) {
					String tmp = bundle.getString(key);
					ArrayList<String> rtn = new ArrayList<String>();
					if (tmp != null) {
						StringTokenizer st = new StringTokenizer(tmp, ",");
						rtn.add(st.nextToken().trim());
					}
					return (T) rtn;
				}

			} catch (Exception e) {
				log.error("Failed to parse " + key + ":"
						+ bundle.getString(key));
			}
		}
		log.warn("Using default value for " + key + ":" + defaultValue);
		return (T) defaultValue;
	}

	public Thread background() {
		GregorianCalendar now = new GregorianCalendar();
		now.add(Calendar.SECOND, getWait());
		setNextRun(now);
		thread = new Thread(this);
		thread.setName(getName());
		thread.start();

		return thread;
	}

	protected void setState(String errStr) {
		log.debug("setState(" + errStr + ")");
		if (errStr != null) {
			this.errStr = errStr;
		}
		setState(errStr == null);
	}

	protected void setState(boolean b) {
		if (b) {
			log.info(getName() + " passed");
			lastOK = new Date();
			lastRunOK = true;
		} else {
			log.info(getName() + " failed");
			lastRunOK = false;
		}

	}

	public boolean killThread() {
		if (thread != null) {
			thread.interrupt();
			return !thread.isAlive();
		}

		return false;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getLastOK() {
		return lastOK;
	}

	public int getWait() {
		return wait;
	}

	public void setWait(int wait) {
		this.wait = wait;
	}

	public GregorianCalendar getNextRun() {
		return nextRun;
	}

	public void setNextRun(GregorianCalendar nextRun) {
		this.nextRun = nextRun;
	}

	public String getErrStr() {
		return errStr;
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	public void setErrStr(String errStr) {
		this.errStr = errStr;
	}

	public void setErrStr(String errStr, Exception e) {
		this.errStr = errStr + ":" + e.getMessage();
		log.error(errStr, e);
	}

	public String getDescription() {
		return description;
	}

	public String getRegion() {
		return region;
	}

	public boolean isLastRunOK() {
		return lastRunOK;
	}

	public void setLastOK(Date lastOK) {
		this.lastOK = lastOK;
	}

	public void setLastRunOK(boolean lastRunOK) {
		this.lastRunOK = lastRunOK;
	}

	public String getDetails() {
		return details;
	}

	public void setDetails(String details) {
		this.details = details;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public BufferedImage getSavedImg() {
		return savedImg;
	}

	public boolean isMutliCheck() {
		return mutliCheck;
	}

	public void setMutliCheck(boolean mutliCheck) {
		this.mutliCheck = mutliCheck;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CheckBase [log=");
		builder.append(log);
		builder.append(", name=");
		builder.append(name);
		builder.append(", description=");
		builder.append(description);
		builder.append(", region=");
		builder.append(region);
		builder.append(", wait=");
		builder.append(wait);
		builder.append(", nextRun=");
		builder.append(nextRun);
		builder.append(", lastOK=");
		builder.append(lastOK);
		builder.append(", lastRunOK=");
		builder.append(lastRunOK);
		builder.append(", errStr=");
		builder.append(errStr);
		builder.append(", bundle=");
		builder.append(bundle);
		builder.append(", running=");
		builder.append(running);
		builder.append(", thread=");
		builder.append(thread);
		builder.append(", contentType=");
		builder.append(contentType);
		builder.append(", details=");
		if (contentType != null && contentType.toLowerCase().contains("text")) {
			builder.append(details);
		} else {
			builder.append("not text");
		}
		builder.append("]");
		return builder.toString();
	}

}
