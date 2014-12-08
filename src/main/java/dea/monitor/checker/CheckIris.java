package dea.monitor.checker;

import java.net.HttpURLConnection;

import org.apache.http.Header;

/**
 * Unfortunately all we can get from the hub is a 200 status code if it is up
 * 
 * @author dea
 * 
 */
public class CheckIris extends CheckUrl {

	public void loadBundle() {
		super.loadBundle();
	}

	public void run() {
		running = true;
		log.info("reading url:" + httpsURL);
		for (int i = 0; i < retries; i++) {
			String s = executeRequest();

			if (respCode == HttpURLConnection.HTTP_OK) {
				setDetails(s);
				setState(null);
				break;
			} else {
				StringBuilder sb = new StringBuilder();
				if (getErrStr() != null) {
					sb.append(getErrStr());
				}
				setState("respCode:" + respCode + " file empty");
				if (getConHeaders() != null) {
					sb.append("Connection Headers:<br>");
					for (String key : getConHeaders().keySet()) {
						sb.append(key).append(":")
								.append(getConHeaders().get(key))
								.append("<br>");
					}
				}
				if (getRespHeaders() != null) {
					sb.append("Response Headers:<br>");
					for (Header header : getRespHeaders()) {
						sb.append(header.getName()).append(":")
								.append(header.getValue()).append("<br>");
					}
				}
				setDetails(sb.toString());
			}
		}
		running = false;
		log.warn("read url");
	}

	public boolean isSaveImage() {
		return saveImage;
	}

	public void setSaveImage(boolean saveImage) {
		this.saveImage = saveImage;
	}

	/**
	 * for testing
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		CheckIris item = new CheckIris();
		item.loadBundle("Iris");
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

}
