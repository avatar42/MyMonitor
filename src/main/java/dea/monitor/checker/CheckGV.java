package dea.monitor.checker;

import java.net.HttpURLConnection;

import org.apache.http.Header;

public class CheckGV extends CheckUrl {
	public static final String GV_NEW_KEY_MARKER = "<input type=\"hidden\" id=\"IDKey\" name=\"IDKey\" value=\"";
	private long minSize = 0;

	public void loadBundle() {
		super.loadBundle();
		minSize = getBundleVal(Long.class, "minSize", minSize);
	}

	public void run() {
		running = true;
		log.info("Checking cam:" + getName());
		for (int i = 0; i < retries; i++) {
			String rsp = login();
			if (rsp.contains("IDS_WEB_ID_PWD_ERROR")) {
				setDetails(rsp);
				setState("Login failed");

			} else {
				// check old software type
				int end = rsp.indexOf("</title>");
				if (end > -1) {
					int start = rsp.lastIndexOf(" ", end);
					if (start > -1) {
						sessionId = rsp.substring(start + 1, end);
					}
				}
				if (sessionId == null || sessionId.contains("<title>")) {
					// check new software type
					int start = rsp.indexOf(GV_NEW_KEY_MARKER);
					if (start > -1) {
						start += GV_NEW_KEY_MARKER.length();
						end = rsp.indexOf("\"", start);
						if (start > -1) {
							sessionId = rsp.substring(start, end);
						}
					}

				}
				String s = executeRequest();

				if (respCode == HttpURLConnection.HTTP_OK && checkResponse(s)) {
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
		}
		running = false;
		log.warn("read url");
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
		CheckGV item = new CheckGV();
		item.cmd(args);
	}

}
