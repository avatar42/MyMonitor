package dea.monitor.checker;

import java.awt.image.BufferedImage;
import java.util.Date;
import java.util.GregorianCalendar;

public interface CheckItemI extends Runnable {
	String getName();

	Date getLastOK();

	boolean isLastRunOK();

	int getWait();

	GregorianCalendar getNextRun();

	void setNextRun(GregorianCalendar nextRun);

	void loadBundle(String bundleName) throws ClassNotFoundException, InstantiationException, IllegalAccessException;

	boolean isRunning();

	Thread background();

	boolean killThread();

	String getErrStr();

	void setErrStr(String errStr);

	public String getDescription();

	String getRegion();

	String getDetails();

	String getContentType();

	void setDetails(String details);

	BufferedImage getSavedImg();

	boolean isMutliCheck();

	// TODO: add notify / beep option

	// common broadcast codes
	float BC_OFFLINE = 0f;
	float BC_EXCEPTION = 100f;
	// 200-507 See HttpStatus
	float BC_OK = 200f;
	float BC_LOGIN_FAILED = 901f;
	float BC_CONTENT_MISSING = 902f;
	float BC_CONTENT_BAD = 903f;
	float BC_CONTENT_OLD = 904f;
	float BC_FAIL_FOUND = 905f;
}
