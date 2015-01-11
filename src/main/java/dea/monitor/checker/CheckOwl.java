package dea.monitor.checker;

import java.net.URI;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;

/**
 * For some reason this does not work though going to via the browser does. More
 * or less.
 * 
 * @author dea
 * 
 */
public class CheckOwl extends CheckUrl {

	public void loadBundle() {
		super.loadBundle();
	}

	protected String executeRequest() {
		String responseStr = null;

		try {
			HttpMessage request = new HttpGet(new URI(httpsURL.toString()));

			// If we get an HTTP 401 Unauthorized with
			// a challenge to solve.
			HttpResponse response = execute((HttpUriRequest) request, context);
			if (respCode == -1) {
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					if (contentType.contains("text")) {
						responseStr = EntityUtils.toString(entity);
						log.info("responseStr:" + responseStr);
					} else if (contentType.contains(OWL_CONTENT_TYPE)) {
						responseStr = EntityUtils.toString(entity);
						log.info("responseStr:" + responseStr);
					} else {
						responseStr = "content type:" + contentType;
					}
					EntityUtils.consume(entity);
				}
			}
		} catch (Exception e) {
			setErrStr("Failed reading URL", e);
			respCode = HttpStatus.SC_GATEWAY_TIMEOUT;
		} finally {
			shutdownClient();
		}

		return responseStr;
	}

	public void run() {
		running = true;
		log.info("reading url:" + httpsURL);
		for (int i = 0; i < retries; i++) {
			String s = executeRequest();

			if (respCode == -1) {
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
		CheckOwl item = new CheckOwl();
		item.loadBundle("Zhuhai");
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
