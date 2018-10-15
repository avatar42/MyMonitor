package dea.monitor.checker;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * The older version of Meteobridge you could use CheckURL with a regular
 * expression but the newer version just returns an response of
 * <meta http-equiv="Refresh" content="0; url=./meteobridge" /> This special
 * check looks at the log for last push status. For example
 * http://192.168.2.139/cgi-bin/eventlog.cgi?1 gets the status of the first push
 * (WU in this case)
 * 
 * @author dea
 * 
 */
public class CheckMeteobridge extends CheckUrl {
	Integer maxOld = 0;
	DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

	public void loadBundle() {
		broadcastType = getBundleVal(String.class, "broadcastType", "wu");
		super.loadBundle();
		maxOld = getBundleVal(Integer.class, "maxOld", maxOld);
	}

	public void run() {
		running = true;
		log.info("reading url:" + httpsURL);
		for (int i = 0; i < retries; i++) {
			String s = getUrl(httpsURL, true);
			/**
			 * response in the form of 2018-10-13 16:12:43 Success: 2018-10-13 16:12:41
			 * 2018-10-13 16:07:40 Success: 2018-10-13 16:07:39 2018-10-13 16:02:38 Success:
			 * 2018-10-13 16:02:37 2018-10-13 15:57:36 Success: 2018-10-13 15:57:35
			 * 2018-10-13 15:52:36 Success: 2018-10-13 15:52:34 2018-10-13 15:47:34 Success:
			 * 2018-10-13 15:47:33 One per line.
			 **/
			log.info(s);

			long lastPost = 0;
			int endIndex = s.indexOf('\n');
			String line = s.substring(0, endIndex);
			if (line.contains("Success:")) {
				String dateStr = line.substring(0, 19);
				try {
					lastPost = (System.currentTimeMillis() - formatter.parse(dateStr).getTime()) / 1000;
					if (lastPost > maxOld) {
						setErrStr("Last post is " + lastPost + " seconds old. " + maxOld + " is max");
						broadcastStatusCode = BC_CONTENT_OLD;
					} else {
						setState(null);
					}
				} catch (ParseException e) {
					setErrStr(e.getMessage());
					broadcastStatusCode = BC_EXCEPTION;
				}
			} else {
				broadcastStatusCode = BC_CONTENT_BAD;
				setErrStr(line);
			}
		}
		broadcastStatus();
		running = false;
		log.warn("read url");
	}

	/**
	 * for testing
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		CheckMeteobridge item = new CheckMeteobridge();
		item.cmd(args);
	}

}
