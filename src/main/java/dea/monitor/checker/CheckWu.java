package dea.monitor.checker;

import org.apache.http.Header;

import twitter4j.JSONException;
import twitter4j.JSONObject;

/**
 * Calls the Wunderground API to get the local stations in a json array
 * 
 * @author dea
 * 
 */
public class CheckWu extends CheckUrl {
	/*
	 * KTXLEAND93: { epoch: 1463711956, ageh: 0, agem: 4, ages: 42, type: "PWS", id:
	 * "KTXLEAND93", lat: "30.57215500", lon: "-97.94791412", adm1: "Leander", adm2:
	 * "TX", country: "US", neighborhood: "Round Mountain Oaks", dateutc:
	 * "2016-05-20 02:34:34", winddir: "-9999", windspeedmph: "-9999.0",
	 * windgustmph: "-999.0", humidity: "99", tempf: "65.5", rainin: "-999.00",
	 * dailyrainin: "-999.00", baromin: "30.07", dewptf: "65.2", weather: "",
	 * clouds: "", windchillf: "-999", heatindexf: "66", softwaretype: "BloomSky",
	 * elev: "922", maxtemp: "75.8", maxtemp_time: " 3:06PM", mintemp: "59.7",
	 * mintemp_time: "7:27AM", maxdewpoint: "71.6", mindewpoint: "59.3",
	 * maxpressure: "30.20", minpressure: "29.99", maxwindspeed: "-9999",
	 * maxwindgust: "-999", maxrain: "-999.00", maxheatindex: "76", minwindchill:
	 * "-999", rtfreq: "2.5", UV: "1232", RawP: "30.0667884153", tzname:
	 * "America/Chicago", CAM:
	 * "http://icons.wunderground.com/webcamramdisk/w/u/wu1::Jt2uqrYDtF1O07eVrpx0ZQ==/2/current.jpg"
	 * , updated: "1463711674" },
	 */
	Integer maxOld = 0;

	public void loadBundle() {
		super.loadBundle();
		maxOld = getBundleVal(Integer.class, "maxOld", maxOld);
	}

	public void run() {
		running = true;
		log.info("reading url:" + httpsURL);
		for (int i = 0; i < retries; i++) {
			JSONObject json = null;
			int lastPost = 0;
			try {
				json = getJson();
				setDetails(json.toString(1));
				JSONObject stations = (JSONObject) json.get("stations");
				JSONObject ws = (JSONObject) stations.get(getName());
				lastPost += (Integer) ws.get("ages");
				lastPost += ((Integer) ws.get("agem")) * 60;
				lastPost += ((Integer) ws.get("ageh")) * 60 * 60;
				if (lastPost > maxOld) {
					setErrStr("Last post is " + lastPost + " seconds old. " + maxOld + " is max");
					broadcastStatusCode = BC_CONTENT_OLD;
				} else {
					setState(null);
				}
			} catch (JSONException e) {
				setErrStr(e.getMessage());
				broadcastStatusCode = BC_EXCEPTION;
			}
			if (json == null) {
				StringBuilder sb = new StringBuilder();
				if (getErrStr() != null) {
					sb.append(getErrStr());
				}
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
		CheckWu item = new CheckWu();
		item.cmd(args);
	}

}
