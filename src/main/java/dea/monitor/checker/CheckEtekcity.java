package dea.monitor.checker;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Iterator;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

/**
 * Call the Vesync API to get the status of the plugs so we can detect when they
 * appear offline to the service.
 * 
 * @author dea
 * 
 */
public class CheckEtekcity extends CheckUrl {
	private String tk;
	private String accountID;
	private static final String BASE_URL = "https://smartapi.vesync.com";

	public static final float ON = 100;
	public static final float OFFLINE = 50;
	public static final float OFF = 0;

	public void loadBundle() {
		super.loadBundle();
		try {
			httpsURL = new URL(BASE_URL);
		} catch (MalformedURLException e) {
			// this can never happen since it it static but got to keep the compiler happy
			e.printStackTrace();
		}
	}

	/**
	 * Log onto the Vesync API server to get and access token.
	 * 
	 * @return true is login was successful otherwise false.
	 * @throws NoSuchAlgorithmException
	 */
	public boolean jsonLogin() throws NoSuchAlgorithmException {
		JSONObject params = new JSONObject();
		params.put("account", login);
		params.put("devToken", "");
		params.put("password", password);

		String url = BASE_URL + "/vold/user/login";
		// get session
		String s = postEtekcity(params, url);

		try {
			// sent response to auth request
			JSONObject resp = JSONObject.fromObject(s);

			tk = resp.getString("tk");
			accountID = resp.getString("accountID");

			return accountID != null && accountID.length() > 1;
		} catch (Exception e) {
			log.error("Faled to parse response:" + s, e);
		}
		return false;
	}

	public String postEtekcity(JSONObject params, String url) {
		HttpResponse response = null;
		String responseStr = null;
		initSSL(httpsURL);
		try {
			HttpPost request = new HttpPost(new URI(url));
			request.setEntity(new StringEntity(params.toString()));
			request.addHeader("content-type", "application/x-www-form-urlencoded");
			response = execute((HttpUriRequest) request, context);
			log.info("login response code:" + respCode);
			if (respCode == HttpStatus.SC_OK || respCode == HttpStatus.SC_MOVED_TEMPORARILY) {
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					log.info("contentType:" + contentType);
					// if (contentType.contains("text")) {
					responseStr = EntityUtils.toString(entity, StandardCharsets.US_ASCII);
					StringBuilder sb = new StringBuilder();
					for (byte b : responseStr.getBytes()) {
						sb.append(String.format("%02x", b));
					}
					log.info("responseStr:" + sb.toString());

					log.info("responseStr:" + responseStr);
					// }
				}
			}
		} catch (URISyntaxException | IOException | ParseException | NoSuchAlgorithmException | KeyManagementException
				| KeyStoreException e) {
			setErrStr("Failed posting URL", e);
		} finally {
			shutdownClient();
		}
		log.info("responseStr:" + responseStr);
		return responseStr;

	}

	/**
	 * Get the basic config info of all the devices on the server
	 * 
	 * @return
	 */
	public JSONArray getDevices() {

		String result = null;
		long startTime = System.currentTimeMillis();
		try {
			URL url = new URL(BASE_URL + "/vold/user/devices");

			HttpURLConnection con = connect(url);
			con.setRequestProperty("tk", tk);
			con.setRequestProperty("accountID", accountID);

			try {
				setRespCode(con.getResponseCode());
			} catch (Exception e) {
				tryTlsPlusCertInstall(con, url, e);
			}
			checkHeaders(con);
			// if connects OK do the read just to be sure
			if (respCode == HttpURLConnection.HTTP_OK || ignoreRespCode) {
				result = getUrlContentAsString(con, true);
			} else if (respCode == HttpURLConnection.HTTP_MOVED_TEMP || ignoreRespCode) {
				result = getUrlContentAsString(con, true);
			} else if (respCode == HttpURLConnection.HTTP_FORBIDDEN || ignoreRespCode) {
				StringBuilder sb = new StringBuilder();
				for (String key : conHeaders.keySet()) {
					sb.append(key).append(":").append(conHeaders.get(key)).append('\n');
				}
				result = sb.toString();

			} else {
				setErrStr("Failed:" + respCode + ": " + con.getResponseMessage());
				setStatusMsg("respCode:" + respCode);
			}
		} catch (Exception e) {
			setErrStr(getName(), e);
		}
		timeToRun = System.currentTimeMillis() - startTime;

		JSONArray resp = JSONArray.fromObject(result);

		log.debug("getDevices response:" + resp);

		return resp;
	}

	public void run() {
		running = true;
		log.info("reading url:" + httpsURL);
		for (int i = 0; i < retries; i++) {
			try {

				if (jsonLogin()) {
					JSONArray data = getDevices();

					broadcastStatusCode = (float) respCode;
					if (respCode == HttpURLConnection.HTTP_OK) {
						setDetails(data.toString());
						setState(null);
						Iterator<?> it = data.iterator();
						while (it.hasNext()) {
							JSONObject jo = (JSONObject) it.next();
							// cid is GUID of device so unique
							String subBundle = getName() + "." + jo.getString("cid");
							String devName = getName() + "." + jo.getString("deviceName");

							if (dbi != null) {
								if (broadcast != null) {
									Map<String, String> devProps = dbi.getItemProperties(subBundle);
									int devBID = 0;
									if (!devProps.containsKey("broadcast.id")) {
										try {
											String devType = "plug";
											// find or create a remote device to link to
											devBID = broadcast.updateDevice(devBID, devName, null, devType, null);
											if (dbi != null) {
												dbi.updateItemProperty(subBundle, "broadcast.id", "" + devBID, true);
												dbi.updateItemProperty(subBundle, "name", devName, true);
											}
										} catch (twitter4j.JSONException | IOException e) {
											e.printStackTrace();
										}
									} else {
										devBID = Integer.parseInt(devProps.get("broadcast.id"));
									}
									try {
										String errMsg = "";
										String statusMsg = "";
										String statusDetails = "";
										float statusCode = -1;
										if ("online".equals(jo.getString("connectionStatus"))) {
											if ("on".equals(jo.getString("deviceStatus"))) {
												statusCode = ON;
											} else {
												statusCode = OFF;
											}
										} else {
											statusCode = OFFLINE;
											errMsg = "Offline";
											statusMsg = "Offline";
											statusDetails = "Offline";
										}

										broadcastStatus(devBID, statusCode, errMsg, statusDetails, statusMsg);
									} catch (UnsupportedOperationException | JSONException e) {
										// throw new InstantiationException(e.getMessage());
									}
								}
							}
						}
						break;
					} else {
						StringBuilder sb = new StringBuilder();
						if (getErrStr() != null) {
							sb.append(getErrStr());
						}
						setState("respCode:" + respCode + " file empty");
						broadcastStatusCode = BC_CONTENT_MISSING;
						if (getConHeaders() != null) {
							sb.append("Connection Headers:<br>");
							for (String key : getConHeaders().keySet()) {
								sb.append(key).append(":").append(getConHeaders().get(key)).append("<br>");
							}
						}
						if (getRespHeaders() != null) {
							sb.append("Response Headers:<br>");
							for (Header header : getRespHeaders()) {
								sb.append(header.getName()).append(":").append(header.getValue()).append("<br>");
							}
						}
						setDetails(sb.toString());
					}
				} else {
					setState("login failed");

				}
			} catch (JSONException | NoSuchAlgorithmException e) {
				setState(e.getMessage());
				log.error("Failed building request", e);
			}
		}
		broadcastStatus();
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
		CheckEtekcity item = new CheckEtekcity();
		item.cmd(args);
	}

}
