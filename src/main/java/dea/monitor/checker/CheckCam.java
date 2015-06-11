package dea.monitor.checker;

import java.net.HttpURLConnection;

import org.apache.http.Header;

public class CheckCam extends CheckUrl {
	private long minSize = 0;

	public void loadBundle() {
		super.loadBundle();
		minSize = getBundleVal(Long.class, "minSize", minSize);
	}

	public void run() {
		running = true;
		log.info("reading url:" + httpsURL);
		for (int i = 0; i < retries; i++) {
			String s = executeRequest();

			if (respCode == HttpURLConnection.HTTP_OK && s != null
					&& len > minSize) {
				setDetails(s);
				setState(null);
				break;
			} else {
				StringBuilder sb = new StringBuilder();
				if (getErrStr() != null) {
					sb.append(getErrStr());
				}
				if (getSavedImg() != null) {
					setState("respCode:" + respCode + " size:"
							+ getSavedImg().getHeight() + " x "
							+ getSavedImg().getWidth() + " " + len + " < "
							+ minSize);
				} else {
					setState("respCode:" + respCode + " file empty");
				}
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

	public long getMinSize() {
		return minSize;
	}

	@Override
	public String toString() {
		return "CheckCam [" + super.toString() + "minSize=" + minSize + "]";
	}

	/**
	 * for testing
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		CheckCam item = new CheckCam();
		item.cmd(args);
	}

}
