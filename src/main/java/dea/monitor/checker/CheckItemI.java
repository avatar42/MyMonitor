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

	void loadBundle(String bundleName);

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
}
