package dea.monitor.checker;

import java.awt.image.BufferedImage;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dea.monitor.db.DBInterface;

public class CheckLogItem implements ChildCheckItemI {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	// config params
	// name of this check
	private String name;
	// what is being checked
	private String description;
	// where is it
	private String region = "home";
	// pattern to denote an error / status off
	private Pattern errPattern;
	// pattern to denote an error cleared / status on
	private Pattern okPattern;
	// what does error found mean
	private String errMsg;
	// what does error cleared mean
	private String okMsg;
	private GregorianCalendar nextRun;
	protected String contentType = "text/text;";
	protected DBInterface dbi;

	// status params
	private Date lastOK;
	private boolean lastRunOK = false;
	private boolean read = true;
	private String errStr = null;
	private String details = null;
	private final Object detailsLock = new Object();
	private final Object errStrLock = new Object();

	public CheckLogItem(String name, String description, String region, String errRegex, String okRegex, String errMsg,
			String okMsg) {
		this.name = name;
		this.description = description;
		this.region = region;
		this.errMsg = errMsg;
		this.okMsg = okMsg;
		if (errRegex != null) {
			errPattern = Pattern.compile(errRegex);
		}
		if (okRegex != null) {
			okPattern = Pattern.compile(okRegex);
		}
		// parent is actually doing the checks so just set this to next year
		nextRun = new GregorianCalendar();
		nextRun.add(Calendar.YEAR, 1);
	}

	public DBInterface getDbi() {
		return dbi;
	}

	public void setDbi(DBInterface dbi) {
		this.dbi = dbi;
	}

	@Override
	public void run() {
		// Not used

	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Date getLastOK() {
		return lastOK;
	}

	@Override
	public boolean isLastRunOK() {
		return lastRunOK;
	}

	@Override
	// not used
	public int getWait() {
		return 0;
	}

	@Override
	public GregorianCalendar getNextRun() {
		return nextRun;
	}

	@Override
	// ignore this
	public void setNextRun(GregorianCalendar nextRun) {
	}

	@Override
	// Not used
	public void loadBundle(String bundleName) {
	}

	@Override
	// always true
	public boolean isRunning() {
		return true;
	}

	@Override
	public Thread background() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	// Not used
	public boolean killThread() {
		return true;
	}

	@Override
	public String getErrStr() {
		String rtn;
		synchronized (errStrLock) {
			rtn = errStr;
		}
		return rtn;
	}

	@Override
	public void setErrStr(String errStr) {
		setErrStr(errStr, new Date());
	}

	public void setErrStr(String errStr, Date d) {
		synchronized (errStrLock) {
			read = false;
			if (errStr == null) {
				lastOK = d;
				lastRunOK = true;
				log.warn("Setting " + getName() + " to:OK:" + getDetails());
			} else {
				lastRunOK = false;
				log.warn("Setting " + getName() + " to:" + errStr);

			}
			this.errStr = errStr;
		}
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getRegion() {
		return region;
	}

	@Override
	public String getDetails() {
		String rtn;
		synchronized (detailsLock) {
			rtn = details;
		}
		return rtn;
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public void setDetails(String details) {
		synchronized (detailsLock) {
			this.details = details;
		}

	}

	public void addDetails(String details) {
		synchronized (detailsLock) {
			this.details += "\n" + details;
		}
	}

	@Override
	public BufferedImage getSavedImg() {
		// Not used
		return null;
	}

	@Override
	// Since is child of MutliCheck is always false
	public boolean isMutliCheck() {
		return false;
	}

	public Pattern getErrPattern() {
		return errPattern;
	}

	public Pattern getOkPattern() {
		return okPattern;
	}

	public void setLastOK(Date lastOK) {
		this.lastOK = lastOK;
	}

	public void setLastRunOK(boolean lastRunOK) {
		this.lastRunOK = lastRunOK;
	}

	public String getErrMsg() {
		return errMsg;
	}

	public String getOkMsg() {
		return okMsg;
	}

	@Override
	public boolean isRead() {
		return read;
	}

	@Override
	public void setRead() {
		read = true;
	}

}
