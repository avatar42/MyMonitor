package dea.monitor.checker;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.MessageDigest;
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
 * Look at camera status on Blue Iris servers. Note uses net.sf.json.JSONObject
 * to avoid issues with duplicate keys in Blue Iris JSON responses.
 * 
 * Note too this checker assumes DB and broadcast options are used
 * 
 * @author dea
 * 
 */
public class CheckBlueIris extends CheckUrl {
	private String session;

	public void loadBundle() {
		super.loadBundle();
	}

	public String postBi(JSONObject params) {
		HttpResponse response = null;
		String responseStr = null;
		initSSL(httpsURL);
		try {
			HttpPost request = new HttpPost(new URI(httpsURL.toString()));
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
	 * Here for looking at iffy responses. For instance something in the data of the
	 * camlist response does not display in eclipse and will not write to a log yet
	 * writes fine to a file and seems to parse to a JSON object fine.
	 * 
	 * @param fileName
	 * @param s
	 * @return
	 */
	public String dumpResponse(String fileName, String s) {
		log.info("s:" + s.length() + ":" + s);
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
			writer.write(s);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (int c = 0; c < s.length(); c++) {
			System.out.println("s[" + c + ":" + s.substring(0, c));
		}
		StringBuilder sb = new StringBuilder();
		int c = 0;
		for (byte b : s.getBytes(StandardCharsets.US_ASCII)) {
			if (b > 31 && b < 127)
				sb.append((char) b);
			else
				System.out.println(String.format("%d:%02x:%d", c, b, b));
			if (c > 4600) {
				log.info(String.format("%d:%02x:%d:(%s)", c, b, b, (char) b));
				System.out.println("sb:" + sb.toString());
				if (c % 100 == 0) {
					System.out.println("sb:" + sb.toString());
				}
			}
			c++;

		}

		return s;
	}

	public String genResponse() throws NoSuchAlgorithmException {
		// var myResponse = md5($("#txtUn").val() + ":" + response.session + ":" +
		// $("#txtPw").val());
		String txt = login + ":" + session + ":" + password;
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] arr = txt.getBytes();
		arr = md.digest(arr);
		StringBuilder sb = new StringBuilder();
		for (byte b : arr) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	public void run() {
		running = true;
		log.info("reading url:" + httpsURL);
		for (int i = 0; i < retries; i++) {
			JSONObject params = new JSONObject();
			try {
				params.put("cmd", "login");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// get session
			String s = postBi(params);

			try {
				// sent response to login challenge
				JSONObject resp = JSONObject.fromObject(s);
				session = resp.getString("session");
				params.put("session", session);
				params.put("response", genResponse());
				s = postBi(params);

				// get camera info
				params.remove("cmd");
				params.put("cmd", "camlist");
				s = postBi(params);
				// dumpResponse("temp.json", s);

				resp = JSONObject.fromObject(s);
				log.info("resp:" + resp);
				JSONArray data = resp.getJSONArray("data");
				log.info("data:" + data.toString());
				broadcastStatusCode = (float) respCode;
				if (respCode == HttpURLConnection.HTTP_OK) {
					setDetails(s);
					setState(null);
					Iterator<?> it = data.iterator();
					while (it.hasNext()) {
						JSONObject jo = (JSONObject) it.next();
						// long name
						String optionDisplay = jo.getString("optionDisplay");
						log.info("optionDisplay:" + optionDisplay);
						// groups start with +
						if (optionDisplay != null && optionDisplay.charAt(0) != '+') {
							// short name
							String optionValue = jo.getString("optionValue");
							String subBundleName = getName() + "." + optionValue;
							if (dbi != null) {
								if (broadcast != null) {
									Map<String, String> camProps = dbi.getItemProperties(subBundleName);
									if (!camProps.containsKey("broadcast.id")) {
										String devType = "cam";
										if (jo.getBoolean("ptz")) {
											devType = "ptz";
										}
										if (jo.getBoolean("audio")) {
											devType = devType + "A";
										}
										try {
											// find or create a remote device to link to
											int camBID = broadcast.updateDevice(0, subBundleName, getName(), devType);
											if (dbi != null) {
												dbi.updateItemProperty(subBundleName, "broadcast.id", "" + camBID);
											}
											float statusCode = 0;
											String errMsg = jo.getString("error");
											String statusMsg = "";
											String statusDetails = "";
											if (!jo.getBoolean("isEnabled")) {
												statusCode = BC_DISABLED;
											} else if (jo.getBoolean("hidden")) {
												statusCode = BC_HIDDEN;
											} else if (!jo.getBoolean("isOnline")) {
												statusCode = BC_NOT_CONNECTED;
											} else if (jo.getBoolean("tempfull")) {
												statusCode = BC_TEMPFULL;
											} else if (jo.getBoolean("isYellow")) {
												statusCode = BC_YELLOW;
											} else if (jo.getBoolean("isNoSignal")) {
												statusCode = BC_NO_SIGNAL;
												statusMsg = jo.getString("nNoSignal") + " Signal drops";
											} else if (jo.getBoolean("isTriggered")) {
												statusCode = BC_TRIGGERED;
												statusMsg = jo.getString("nTriggers") + " Triggers";
											} else if (jo.getBoolean("isMotion")) {
												statusCode = BC_MOTION;
												statusMsg = jo.getString("nAlerts") + " Alerts";
											} else if (jo.getBoolean("isPaused")) {
												statusCode = BC_PAUSED;
												statusMsg = jo.getString("pause") + " Seconds";
											} else if (jo.getBoolean("isRecording")) {
												statusCode = BC_RECORDING;
												statusMsg = jo.getString("FPS") + " FPS";
											} else if (jo.getBoolean("isOnline")) {
												statusCode = BC_CONNECTED;
												statusMsg = jo.getString("nClips") + " Clips";
											}

											broadcastStatus(camBID, statusCode, errMsg, statusDetails, statusMsg);
										} catch (UnsupportedOperationException | JSONException | IOException e) {
											// throw new InstantiationException(e.getMessage());
										} catch (twitter4j.JSONException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
									}
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
		CheckBlueIris item = new CheckBlueIris();
		item.cmd(args);
	}

}
