package dea.monitor.checker;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

@SuppressWarnings("restriction")
public class CheckHttpHandler extends CheckBase implements HttpHandler {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private String parm;
	// a list of the values to be taken as errors
	private List<String> errors;
	// a list of the values to be taken as return to OK
	private List<String> clears;

	/*
	 * @see
	 * com.sun.net.httpserver.HttpHandler#handle(com.sun.net.httpserver.HttpExchange
	 * )
	 */
	public void handle(HttpExchange exchange) throws IOException {
		log.info("in handler()");
		StringBuilder sb = new StringBuilder();

		sb.append("From:").append(exchange.getRemoteAddress()).append("\n");
		String requestMethod = exchange.getRequestMethod();
		sb.append("Method:").append(requestMethod).append("\n");
		if (requestMethod.equalsIgnoreCase("GET")) {
			Headers responseHeaders = exchange.getResponseHeaders();
			responseHeaders.set("content-Type", "text/plain");
			// 0= dynamic length
			exchange.sendResponseHeaders(HttpStatus.SC_OK, 0);

			OutputStream responseBody = exchange.getResponseBody();

			sb.append("Request headers:\n");
			Headers requestHeaders = exchange.getRequestHeaders();
			Set<String> keySet = requestHeaders.keySet();
			Iterator<String> iter = keySet.iterator();
			while (iter.hasNext()) {
				String key = iter.next();
				List<String> values = requestHeaders.get(key);
				sb.append(key).append(" = ").append(values.toString())
						.append("\n");
			}
			URI uri = exchange.getRequestURI();
			sb.append("URI:").append(uri).append("\n");
			sb.append("URI.path:").append(uri.getPath()).append("\n");
			sb.append("URI.query:").append(uri.getQuery()).append("\n");
			sb.append("URI.user:").append(uri.getUserInfo()).append("\n");
			StringTokenizer st = new StringTokenizer(uri.getQuery(), "&");
			while (st.hasMoreTokens()) {
				String ps = st.nextToken();
				int idx = ps.indexOf('=');
				String p;
				String v = null;
				if (idx > -1) {
					p = ps.substring(0, idx);
				} else {
					p = ps;
				}

				if (idx > -1) {
					idx++;
					if (idx < ps.length()) {
						v = ps.substring(idx, ps.length());
					}
				} else {
					sb.append(ps);
				}
				sb.append("parm:(").append(parm).append(")\n");
				sb.append("(").append(p).append(") = (").append(v)
						.append(")\n");
				if (parm.equals(p)) {
					if (errors.contains(v)) {
						setState(p + " = " + v);
					} else if (clears.contains(v)) {
						setState(null);
					} else {
						// TODO: set unknown?
						log.info("unknown state");
					}
				}
			}
			sb.append("Response headers:\n");
			keySet = responseHeaders.keySet();
			iter = keySet.iterator();
			while (iter.hasNext()) {
				String key = iter.next();
				List<String> values = responseHeaders.get(key);
				sb.append(key).append(" = ").append(values.toString())
						.append("\n");
			}
			String debug = sb.toString();
			log.info(debug);

			responseBody.write("OK".getBytes());
			responseBody.close();
		}

		exchange.close();
	}

	/**
	 * Ignored, handlers are run in the listening server instead of polling.
	 * 
	 */
	@Override
	public void run() {
		// ignored
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void loadBundle() {
		parm = getBundleVal(String.class, "parm", "val");
		errors = getBundleVal(ArrayList.class, "errors", null);
		clears = getBundleVal(ArrayList.class, "clears", null);
	}

}
