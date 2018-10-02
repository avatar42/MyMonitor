package dea.monitor.checker;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
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
		ignoreRespCode = true;
	}

	protected String executeRequest(String url) {
		String responseStr = null;

		try {
			HttpPost httpGet = new HttpPost(new URI(httpsURL.toString()));

			// If we get an HTTP 401 Unauthorized with
			// a challenge to solve.
			// HttpResponse response = execute((HttpUriRequest) request,
			// context);
			final HttpUriRequest request = (HttpUriRequest) httpGet;
			httpclient = HttpClients
					.custom()
					.setUserAgent(
							"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.81 Safari/537.36")
					.build();
			// .setDefaultCookieStore(cookieStore)
			// Add your Data
			final List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
					6);
			nameValuePairs
					.add(new BasicNameValuePair("Accept: ",
							"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"));
			nameValuePairs.add(new BasicNameValuePair("Accept-Encoding: ",
					"gzip, deflate, sdch"));
			nameValuePairs.add(new BasicNameValuePair("Accept-Language: ",
					"en-US,en;q=0.8"));
			// nameValuePairs.add(new BasicNameValuePair("Content-Length: ",
			// "6"));

			httpGet.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			log.info("Doing " + request.getMethod() + " to " + request.getURI());
			checkHeaders(request);
			// Create a custom response handler
			ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

				@Override
				public String handleResponse(final HttpResponse response)
						throws ClientProtocolException, IOException {
					int status = response.getStatusLine().getStatusCode();
					try {
						checkHeaders(response, request.getURI());
					} catch (ParseException e) {
						throw new IOException(e);
					}
					setRespCode(response.getStatusLine().getStatusCode());
					
					if (respCode == -1) {
						HttpEntity entity = response.getEntity();
						String responseStr = null;
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
					if (status >= 200 && status < 300) {
						HttpEntity entity = response.getEntity();
						return entity != null ? EntityUtils.toString(entity)
								: null;
					} else {
						throw new ClientProtocolException(
								"Unexpected response status: " + status);
					}
				}

			};
			responseStr = httpclient.execute(request, responseHandler);
		} catch (Exception e) {
			setErrStr("Failed reading URL", e);
			setRespCode(HttpStatus.SC_GATEWAY_TIMEOUT);
			try {
				URLConnection conHand = httpsURL.openConnection();
				String type = conHand.getContentType();
				if (type == null)
					type = "unknown";

				log.debug("type:" + type);
				responseStr = getUrlContentAsString((HttpURLConnection) conHand);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} finally {
			shutdownClient();
		}

		return responseStr;
	}

	public void run() {
		running = true;
		log.info("reading url:" + httpsURL);
		for (int i = 0; i < retries; i++) {
			String s = executeRequest(httpsURL);

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
		item.cmd(args);
	}

}
