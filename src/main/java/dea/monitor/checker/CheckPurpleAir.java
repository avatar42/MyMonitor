package dea.monitor.checker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;

import org.apache.http.Header;

import twitter4j.JSONArray;
import twitter4j.JSONException;
import twitter4j.JSONObject;

/**
 * Look at PurpleAir sensor data AQI convertions see
 * https://forum.airnowtech.org/t/the-aqi-equation/169 Turn off intake when AVG
 * PM2.5 PM1.0 or PM10 > 10
 * 
 * Note the data saved by setting saveData to true can be downloaded in even greater detail at 
 * http://www.purpleair.com/sensorlist?fbclid=IwAR2Zhsa6ncdFziQ3ggxKYdn2oG3utn5HGNFjiA0dMllUu9R3xndmMYXdirk
 * 
 * @author avatar42
 *
 */
public class CheckPurpleAir extends CheckUrl {
	private boolean saveData = false;
	private Integer maxOld = 0;
	private String outPath;
	private String localURL;

	public void loadBundle() {
		broadcastType = getBundleVal(String.class, "broadcastType", "wu");
		super.loadBundle();

		saveData = getBundleVal(Boolean.class, "saveData", saveData);
		outPath = getBundleVal(String.class, "exportPath", getName() + ".csv");
		localURL = getBundleVal(String.class, "localURL", localURL);
		maxOld = getBundleVal(Integer.class, "maxOld", maxOld);
	}

	private void processSensor(String subBundleName, float statusCode, String devType) {

		if (dbi != null) {
			if (broadcast != null) {

				Map<String, String> devProps = dbi.getItemProperties(subBundleName);
				int devBID = 0;
				if (!devProps.containsKey("broadcast.id")) {
					try {
						// find or create a remote device to link to
						devBID = broadcast.updateDevice(devBID, subBundleName, getRegion(), devType, null);
						if (dbi != null) {
							dbi.insertItemProperty(subBundleName, "broadcast.id", "" + devBID, true);
						}
					} catch (twitter4j.JSONException | IOException e) {
						e.printStackTrace();
					}
				} else {
					devBID = Integer.parseInt(devProps.get("broadcast.id"));
				}
				String errMsg = "";
				String statusMsg = "";
				String statusDetails = "";
				broadcastStatus(devBID, statusCode, errMsg, statusDetails, statusMsg);
			}
		}

	}

	public void run() {
		running = true;
		log.info("reading url:" + httpsURL);
		for (int i = 0; i < retries; i++) {
			JSONObject json = null;
			StringBuilder sb = null;
			FileWriter writer = null;
			try {
				GregorianCalendar gc = new GregorianCalendar();
				json = getJson();
				if (saveData) {
					sb = new StringBuilder();
				}
				setDetails(json.toString(1));
				JSONArray sensors = (JSONArray) json.get("results");

				JSONObject jo = (JSONObject) sensors.get(0);
				String label = "A";
				Long lastPostA = jo.getLong("LastSeen");

				int rssi = (Integer) jo.getInt("RSSI");
				processSensor(getName() + "." + label + ".RSSI", rssi, "rssi");

				float pm2 = Float.parseFloat(jo.getString("PM2_5Value"));
				processSensor(getName() + "." + label + ".PM2.5", pm2, "cnt");

				float temp = Float.parseFloat(jo.getString("temp_f"));
				processSensor(getName() + "." + label + ".temp", temp, "temp");

				float humidity = Float.parseFloat(jo.getString("humidity"));
				processSensor(getName() + "." + label + ".humidity", humidity, "humidity");

				float pressure = Float.parseFloat(jo.getString("pressure"));
				processSensor(getName() + "." + label + ".pressure", pressure, "pressure");

				if (saveData) {
					sb = sb.append(lastPostA).append(",").append(rssi).append(",").append(pm2).append(",").append(temp)
							.append(",").append(humidity).append(",").append(pressure).append(",");
				}

				jo = (JSONObject) sensors.get(1);
				label = "B";
				Long lastPostB = jo.getLong("LastSeen");

				rssi = (Integer) jo.getInt("RSSI");
				processSensor(getName() + "." + label + ".RSSI", rssi, "rssi");

				pm2 = Float.parseFloat(jo.getString("PM2_5Value"));
				processSensor(getName() + "." + label + ".PM2.5", pm2, "cnt");

				temp = Float.parseFloat(jo.getString("temp_f"));
				processSensor(getName() + "." + label + ".temp", temp, "temp");

				humidity = Float.parseFloat(jo.getString("humidity"));
				processSensor(getName() + "." + label + ".humidity", humidity, "humidity");

				pressure = Float.parseFloat(jo.getString("pressure"));
				processSensor(getName() + "." + label + ".pressure", pressure, "pressure");

				if (saveData) {
					sb = sb.append(lastPostB).append(",").append(rssi).append(",").append(pm2).append(",").append(temp)
							.append(",").append(humidity).append(",").append(pressure).append(",\n");
					try {
						File outFile = new File(
								outPath + "." + gc.get(Calendar.YEAR) + "." + (gc.get(Calendar.MONTH) + 1) + ".csv");
						boolean addHeader = !outFile.exists();
						writer = new FileWriter(outFile, true);
						if (addHeader) {
							writer.write(
									"lastPostA,rssiA,pm2A,tempA,humidityA,pressureA,lastPostB,rssiB,pm2B,tempB,humidityB,pressureB,\n");
						}
						writer.write(sb.toString());
						writer.flush();
					} finally {
						if (writer != null) {
							writer.close();
						}
					}

					json = getJson(new URL(localURL));
					try {
						File outFile = new File(outPath + ".local." + gc.get(Calendar.YEAR) + "."
								+ (gc.get(Calendar.MONTH) + 1) + ".csv");
						boolean addHeader = !outFile.exists();
						writer = new FileWriter(outFile, true);
						if (addHeader) {
							writer.write(
									"SensorId,DateTime,Geo,Mem,Id,Adc,lat,lon,accuracy,elevation,version,uptime,rssi,hardwareversion,hardwarediscovered,current_temp_f,current_humidity,current_dewpoint_f,pressure,pm1_0_atm_b,pm2_5_atm_b,pm10_0_atm_b,pm1_0_cf_1_b,pm2_5_cf_1_b,pm10_0_cf_1_b,p_0_3_um_b,p_0_5_um_b,p_1_0_um_b,p_2_5_um_b,p_5_0_um_b,p_10_0_um_b,pm1_0_atm,pm2_5_atm,pm10_0_atm,pm1_0_cf_1,pm2_5_cf_1,pm10_0_cf_1,p_0_3_um,p_0_5_um,p_1_0_um,p_2_5_um,p_5_0_um,p_10_0_um,responseCode,responseCode_date,key1_responseCode,key1_responseCode_date,key1_count,key2_responseCode,key2_responseCode_date,key2_count,key1_responseCode_b,key1_responseCode_date_b,key1_count_b,key2_responseCode_b,key2_responseCode_date_b,key2_count_b\n");
						}
						writer.write(json.getString("SensorId") + ",");
						writer.write(json.getString("DateTime") + ",");
						writer.write(json.getString("Geo") + ",");
						writer.write(json.getString("Mem") + ",");
						writer.write(json.getString("Id") + ",");
						writer.write(json.getString("Adc") + ",");
						writer.write(json.getString("lat") + ",");
						writer.write(json.getString("lon") + ",");
						writer.write(json.getString("accuracy") + ",");
						writer.write(json.getString("elevation") + ",");
						writer.write(json.getString("version") + ",");
						writer.write(json.getString("uptime") + ",");
						writer.write(json.getString("rssi") + ",");
						writer.write(json.getString("hardwareversion") + ",");
						writer.write(json.getString("hardwarediscovered") + ",");
						writer.write(json.getString("current_temp_f") + ",");
						writer.write(json.getString("current_humidity") + ",");
						writer.write(json.getString("current_dewpoint_f") + ",");
						writer.write(json.getString("pressure") + ",");
						writer.write(json.getString("pm1_0_atm_b") + ",");
						writer.write(json.getString("pm2_5_atm_b") + ",");
						writer.write(json.getString("pm10_0_atm_b") + ",");
						writer.write(json.getString("pm1_0_cf_1_b") + ",");
						writer.write(json.getString("pm2_5_cf_1_b") + ",");
						writer.write(json.getString("pm10_0_cf_1_b") + ",");
						writer.write(json.getString("p_0_3_um_b") + ",");
						writer.write(json.getString("p_0_5_um_b") + ",");
						writer.write(json.getString("p_1_0_um_b") + ",");
						writer.write(json.getString("p_2_5_um_b") + ",");
						writer.write(json.getString("p_5_0_um_b") + ",");
						writer.write(json.getString("p_10_0_um_b") + ",");
						writer.write(json.getString("pm1_0_atm") + ",");
						writer.write(json.getString("pm2_5_atm") + ",");
						writer.write(json.getString("pm10_0_atm") + ",");
						writer.write(json.getString("pm1_0_cf_1") + ",");
						writer.write(json.getString("pm2_5_cf_1") + ",");
						writer.write(json.getString("pm10_0_cf_1") + ",");
						writer.write(json.getString("p_0_3_um") + ",");
						writer.write(json.getString("p_0_5_um") + ",");
						writer.write(json.getString("p_1_0_um") + ",");
						writer.write(json.getString("p_2_5_um") + ",");
						writer.write(json.getString("p_5_0_um") + ",");
						writer.write(json.getString("p_10_0_um") + ",");
						writer.write(json.getString("responseCode") + ",");
						writer.write(json.getString("responseCode_date") + ",");
						writer.write(json.getString("key1_responseCode") + ",");
						writer.write(json.getString("key1_responseCode_date") + ",");
						writer.write(json.getString("key1_count") + ",");
						writer.write(json.getString("key2_responseCode") + ",");
						writer.write(json.getString("key2_responseCode_date") + ",");
						writer.write(json.getString("key2_count") + ",");
						writer.write(json.getString("key1_responseCode_b") + ",");
						writer.write(json.getString("key1_responseCode_date_b") + ",");
						writer.write(json.getString("key1_count_b") + ",");
						writer.write(json.getString("key2_responseCode_b") + ",");
						writer.write(json.getString("key2_responseCode_date_b") + ",");
						writer.write(json.getString("key2_count_b") + ",\n");
						writer.flush();
					} finally {
						if (writer != null) {
							writer.close();
						}
					}

				}

				if ((System.currentTimeMillis() / 1000) - lastPostA > maxOld) {
					setErrStr("Last post A is " + lastPostA + " seconds old. " + maxOld + " is max");
					broadcastStatusCode = BC_CONTENT_OLD;
				} else if ((System.currentTimeMillis() / 1000) - lastPostB > maxOld) {
					setErrStr("Last post B is " + lastPostB + " seconds old. " + maxOld + " is max");
					broadcastStatusCode = BC_CONTENT_OLD;
				} else {
					setState(null);
				}
				break;
			} catch (JSONException | IOException e) {
				setErrStr(e.getMessage());
				broadcastStatusCode = BC_EXCEPTION;
			}
			if (json == null) {
				sb = new StringBuilder();
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
		CheckPurpleAir item = new CheckPurpleAir();
		item.cmd(args);
	}

}
