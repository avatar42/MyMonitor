package dea.monitor.checker;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dea.monitor.broadcast.BroadcastInterface;
import dea.monitor.db.DBInterface;
import dea.monitor.reset.ResetI;
import dea.monitor.tools.Props2DB;
import twitter4j.JSONException;

public abstract class CheckBase implements CheckItemI {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	// set to true when making code changes to update all current objects to latest
	// style first loop thru
	protected boolean forceUpdate = true;

	private String name;
	private String description;
	private String region = "home";
	private int wait = 1800; // seconds between checks
	private GregorianCalendar nextRun = new GregorianCalendar();
	private Date lastOK;
	private boolean lastRunOK = false;
	private String errStr = null;
	private String statusMsg = null;
	private String details = null;
	protected ResourceBundle bundle;
	protected Map<String, String> props;
	protected boolean running = false;
	protected Thread thread;
	protected String contentType = "text/text;";
	protected BufferedImage savedImg;
	// has child items that are checked
	protected boolean mutliCheck = false;
	// Class to use to broadcast status
	protected BroadcastInterface broadcast;
	// ID in the remote system
	protected int broadcastID = 0;
	// last broadcast status code
	protected Float broadcastStatusCode = BC_OFFLINE;
	// last broadcast status string
	protected String broadcastStatusString = null;
	// type of device on the receiving system
	protected String broadcastType = "web";
	// if not null the custom address to use for the remote device.
	protected String broadcastAddr;
	// DB interface class
	protected DBInterface dbi;
	// Reset interface class
	protected ResetI reset;

	protected abstract void loadBundle();

	/**
	 * See Interface
	 */
	public void loadBundle(String bundleName)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		if (dbi == null) {
			bundle = ResourceBundle.getBundle(bundleName);
		} else {
			props = dbi.getItemProperties(bundleName);
			if (props.isEmpty()) {
				// if using DB but no props found then add to the DB
				try {
					Props2DB p2b = new Props2DB();
					p2b.addAllPropsToDB(bundleName, this.getClass().getName());
					props = dbi.getItemProperties(bundleName);
				} catch (SQLException e) {
					throw new InstantiationException(e.getMessage());
				}
			}
		}

		name = bundleName;

		wait = getBundleVal(Integer.class, "wait", wait);
		description = getBundleVal(String.class, "description", name);
		region = getBundleVal(String.class, "region", region);
		// load child props and broadcastType
		loadBundle();

		String clsStr = getBundleVal(String.class, "reset.class", null);
		if (clsStr != null) {
			Class<?> hiClass = Class.forName(clsStr);
			reset = (ResetI) hiClass.newInstance();
		}
		clsStr = getBundleVal(String.class, "broadcast.class", null);

		if (clsStr != null) {
			Class<?> hiClass = Class.forName(clsStr);
			broadcast = (BroadcastInterface) hiClass.newInstance();
			broadcastID = getBundleVal(Integer.class, "broadcast.id", 0);
			try {
				int bid = broadcast.updateDevice(broadcastID, name, region, broadcastType, broadcastAddr, null);
				if (broadcastID == 0) {
					broadcastID = bid;
					updateProp("broadcast.id", broadcastID);
				}
			} catch (UnsupportedOperationException | JSONException | IOException e) {
				throw new InstantiationException(e.getMessage());
			}
		}
	}

	public DBInterface getDbi() {
		return dbi;
	}

	public void setDbi(DBInterface dbi) {
		this.dbi = dbi;
	}

	/**
	 * Update or adds property in DB and local Map copy
	 * 
	 * @param key
	 * @param newValue
	 * @return number of records changed
	 */
	protected int updateProp(String key, Object newValue) {
		int cnt = 0;
		if (dbi != null && newValue != null) {
			cnt = dbi.updateItemProperty(name, key, newValue.toString(), true);
			props.put(key, newValue.toString());
		}
		return cnt;
	}

	/**
	 * Convert string value to a class of type asClass
	 * 
	 * @param asClass
	 * @param stringVal a String of the word null is treated the same as null
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private <T> T stringToCls(Class<T> asClass, String stringVal) {

		if (stringVal != null) {
			if (Integer.class.isAssignableFrom(asClass))
				return (T) new Integer(stringVal);

			if (Long.class.isAssignableFrom(asClass))
				return (T) new Long(stringVal);

			if (Boolean.class.isAssignableFrom(asClass))
				return (T) new Boolean(stringVal);

			if (String.class.isAssignableFrom(asClass))
				return (T) stringVal;

			if (ArrayList.class.isAssignableFrom(asClass)) {
				String tmp = stringVal;
				ArrayList<String> rtn = new ArrayList<String>();
				if (tmp != null) {
					StringTokenizer st = new StringTokenizer(tmp, ",");
					rtn.add(st.nextToken().trim());
				}
				return (T) rtn;
			}
		}

		return null;
	}

	/**
	 * Get value from bundle or DB. Supported types are Integer, Long, Boolean or
	 * ArrayList<String> Note props files will used if available. If not the the DB
	 * version will be.
	 * 
	 * @param asClass      Class you want back
	 * @param key          to look for in the properties.
	 * @param defaultValue to return if not found
	 * @return property requested for the item
	 */
	protected <T> T getBundleVal(Class<T> asClass, String key, T defaultValue) {
		T rtn = null;
		if (bundle != null && bundle.containsKey(key)) {
			try {
				rtn = stringToCls(asClass, bundle.getString(key));
			} catch (Exception e) {
				log.error("Failed to parse " + key + ":" + bundle.getString(key));
			}
		} else if (props != null && props.containsKey(key)) {
			try {
				rtn = stringToCls(asClass, props.get(key));
			} catch (Exception e) {
				log.error("Failed to parse " + key + ":" + props.get(key));
			}

		}
		if (rtn != null) {
			if ("null".equals(rtn)) {
				updateProp(key, defaultValue);
				return (T) defaultValue;
			} else {
				return rtn;
			}
		}

		log.warn("Using default value for " + key + ":" + defaultValue);
		// if not found then add default to DB is using DB.
		updateProp(key, defaultValue);

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

	protected void broadcastStatus() {
		broadcastStatus(broadcastID, broadcastStatusCode, errStr, details, statusMsg);
	}

	//TODO: only send changed values?
	//TODO: add save sent data to DB option
	protected void broadcastStatus(int bid, float statusCode, String errMsg, String statusDetails, String statusMsg) {
		if (broadcast != null && bid > 0) {
			try {
				broadcast.sendVal(bid, statusCode);
				broadcast.sendError(bid, errMsg);
				broadcast.sendStatusString(bid, statusMsg);
				broadcast.sendDetails(bid, statusDetails);
			} catch (UnsupportedOperationException | IOException e) {
				log.error("Failed sending status", e);
				setState("Failed sending status");
			}
		}
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
		setState(errStr == null);
	}

	public void setErrStr(String errStr, Exception e) {
		this.errStr = errStr + ":" + e.getMessage();
		log.error(errStr, e);
	}

	public String getStatusMsg() {
		return statusMsg;
	}

	public void setStatusMsg(String statusMsg) {
		this.statusMsg = statusMsg;
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

	public void setDetailsAsHtml(String details) {

		this.details = details.replaceAll("\n", "<br>");
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

	public void usage() {
		System.err.println("Usage: " + getClass().getName() + " bundleName [+b|-b|-d|-e]");
		System.err.println("+b = add broadcast.class (default.broadcast.class) to bundleName's properties");
		System.err.println("-b = disable broadcast.class to bundleName's properties");
		System.err.println("-e = disable all bundleName's properties");
		System.err.println("+e = reenable all bundleName's properties");

	}

	/**
	 * Generic command line method to be called from main in checkers
	 * 
	 * @param args
	 */
	public void cmd(String[] args) {
		String bundleName = null;
		if (args.length > 0) {
			try {
				Props2DB p2d = new Props2DB();
				bundleName = p2d.parse(getClass().getCanonicalName(), args);
				dbi = p2d.getDbi();
			} catch (SQLException e1) {
				log.error("Failed connectiong to DB", e1);
			}
			try {
				loadBundle(bundleName);
				Thread thread = background();

				while (thread.isAlive()) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				usage();
				log.error("Failed to create checker", e);
			}
			log.info("done:" + getErrStr());
			log.info("end:" + toString());
		} else {
			usage();
		}

	}

}
