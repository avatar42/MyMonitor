package dea.monitor.checker;

import java.awt.image.BufferedImage;
import java.util.Date;
import java.util.GregorianCalendar;

import dea.monitor.db.DBInterface;

public interface CheckItemI extends Runnable {

	/**
	 * load the properties for an item from the DB id dbi is set otherwise from
	 * property files
	 * 
	 * @param bundleName
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	void loadBundle(String bundleName) throws ClassNotFoundException, InstantiationException, IllegalAccessException;

	/**
	 * Is check running
	 * 
	 * @return
	 */
	boolean isRunning();

	/**
	 * Run check in a Thread
	 * 
	 * @return
	 */
	Thread background();

	/**
	 * Stop a check from running
	 * 
	 * @return
	 */
	boolean killThread();

	/**
	 * Get name of Check item
	 * 
	 * @return
	 */
	String getName();

	/**
	 * Get time of next run
	 * 
	 * @return
	 */
	GregorianCalendar getNextRun();

	/**
	 * Set time of next run
	 * 
	 * @param nextRun
	 */
	void setNextRun(GregorianCalendar nextRun);

	/**
	 * Get when last successfully ran check
	 * 
	 * @return
	 */
	Date getLastOK();

	/**
	 * Was last check successful
	 * 
	 * @return
	 */
	boolean isLastRunOK();

	/**
	 * If true this check displays as multiple items.
	 * 
	 * @return
	 */
	boolean isMutliCheck();

	/**
	 * Get cause to last check failure
	 * 
	 * @return
	 */
	String getErrStr();

	/**
	 * Set cause to last check failure
	 * 
	 * @param errStr
	 */
	void setErrStr(String errStr);

	/**
	 * Get extra data stored to help with debugging
	 * 
	 * @return
	 */
	String getDetails();

	/**
	 * Set extra data stored to help with debugging
	 * 
	 * @param details
	 */
	void setDetails(String details);

	/**
	 * Get response content type of last URL check
	 * 
	 * @return
	 */
	String getContentType();

	/**
	 * If the response content type was image this returns that image.
	 * 
	 * @return
	 */
	BufferedImage getSavedImg();

	/**
	 * Get time to wait between checks in seconds Set from wait property
	 * 
	 * @return
	 */
	int getWait();

	/**
	 * Get the description property
	 * 
	 * @return
	 */
	public String getDescription();

	/**
	 * Get the region property. Used to group items in GUI
	 * 
	 * @return
	 */
	String getRegion();

	/**
	 * Give DB connection to use instead of props files
	 * 
	 * @param dbi
	 */
	void setDbi(DBInterface dbi);

	// common broadcast codes
	float BC_OFFLINE = 0f;
	// device connected
	float BC_CONNECTED = 1f;
	// device not connected
	float BC_NOT_CONNECTED = 2f;
	// Camera has been triggered
	float BC_TRIGGERED = 3f;
	// Camera is recording
	float BC_RECORDING = 4f;
	// Camera detecting motion
	float BC_MOTION = 5f;
	// Camera is paused
	float BC_PAUSED = 6f;
	// camera is hidden
	float BC_HIDDEN = 7f;
	// temp area full
	float BC_TEMPFULL = 8f;
	// camera has no signal
	float BC_NO_SIGNAL = 9f;
	// camera is disabled
	float BC_DISABLED = 10f;
	//when motion detection is paused or disabled (low frame rate)
	float BC_YELLOW = 11f;

	// SSH error
	float BC_SSH_ERROR = 20f;
	
	float BC_EXCEPTION = 100f;
	
	// 200-507 See HttpStatus
	float BC_OK = 200f;
	
	// 900 - 999 MyMonitor checker error
	float BC_LOGIN_FAILED = 901f;
	float BC_CONTENT_MISSING = 902f;
	float BC_CONTENT_BAD = 903f;
	float BC_CONTENT_OLD = 904f;
	float BC_FAIL_FOUND = 905f;
}
