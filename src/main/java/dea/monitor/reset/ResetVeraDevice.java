package dea.monitor.reset;

import java.net.URL;

import dea.monitor.checker.CheckUrl;

/**
 * Send a command to a Vera hub to cycle a switch connected to the power of the
 * failing device by sending a device off then on the waiting a set number of
 * seconds using a URL like
 * http://192.168.1.87:3480/data_request?id=action&output_format
 * =xml&DeviceNum=77
 * &serviceId=urn:upnp-org:serviceId:SwitchPower1&action=SetTarget
 * &newTargetValue=0
 * 
 * @author dea
 * 
 */
public class ResetVeraDevice extends CheckUrl implements ResetI {
	protected URL stopURL;
	protected URL startURL;
	// seconds to wait after reset before restart
	protected int resetPause = 2;
	// seconds to wait after reset before next check
	protected int resetWait = 2;

	public void loadBundle() {
		stopURL = getURL("reset.Url", "0");
		startURL = getURL("reset.Url", "1");
		resetPause = getBundleVal(Integer.class, "reset.pause", resetWait);
		resetWait = getBundleVal(Integer.class, "reset.wait", resetWait);
	}

	/**
	 * Generic command line method to be called from main in checkers
	 * 
	 * @param args
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws ClassNotFoundException
	 */
	public void doReset(String bundleName) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException {
		loadBundle(bundleName);
		// call switch off
		// if the check failed and we have a reset URL run it before
		// next
		// check
		if (stopURL != null) {
			log.info("Stopping:" + bundleName);
			String s = executeRequest(stopURL);
			log.info(s);
			try {
				Thread.sleep(resetPause * 1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// call switch on
		if (startURL != null) {
			log.info("Starting:" + bundleName);
			String s = executeRequest(startURL);
			log.info(s);
			try {
				Thread.sleep(resetWait * 1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		log.info("done:" + getErrStr());
		log.info("end:" + toString());

	}

	/**
	 * for testing
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ResetI item = new ResetVeraDevice();
		if (args.length > 0) {
			try {
				item.doReset(args[0]);
			} catch (Exception e) {
				item.usage();
				e.printStackTrace();
			}
		} else {
			item.usage();
		}
	}

}
