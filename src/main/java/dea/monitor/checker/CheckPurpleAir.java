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
 * Note the data saved by setting saveData to true can be downloaded in even
 * greater detail at
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

	/**
	 * get and broadcast value from local web service.
	 * @param jo
	 * @param jsonName
	 * @return value as String with appended ,
	 */
	private String processSensorCnt(JSONObject jo, String jsonName) {
		return processSensor(jo, jsonName, getName() + "." + jsonName, "cnt") +",";
	}

	private String processSensor(JSONObject jo, String jsonName, String subBundleName, String devType) {
		String val = "-";
		try {
			val = jo.getString(jsonName);
			float pm2 = Float.parseFloat(val);
			processSensor(subBundleName, pm2, "cnt");
		} catch (Exception e) {
			// if the value is no longer in the json log a - but do not broadcast
			// if found but unable to parse then log as string but do not broadcast
			log.warn("got " + val + " for " + jsonName + " but was unable to send to server:" + e.getMessage());
		}

		return val;
	}

	private void processSensor(String subBundleName, float statusCode, String devType) {

		if (dbi != null) {
			if (broadcast != null) {

				Map<String, String> devProps = dbi.getItemProperties(subBundleName);
				int devBID = 0;
				if (!devProps.containsKey("broadcast.id")) {
					try {
						// find or create a remote device to link to
						devBID = broadcast.updateDevice(devBID, subBundleName, getRegion(), devType, null, null);
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

	/**
	 * Deal with removed parms
	 * 
	 * @param json JSONObject from response
	 * @param name of parm to grab
	 * @return
	 */
	private String getParmString(JSONObject json, String name) {
		try {
			return json.getString(name) + ",";
		} catch (JSONException e) {
			log.warn("parm " + name + "missing from JSON");
			return ",";
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

				String rssi = processSensor(jo, "RSSI", getName() + "." + label + ".RSSI", "rssi");
				String pm2 = processSensor(jo, "PM2_5Value", getName() + "." + label + ".PM2.5", "cnt");
				String temp = processSensor(jo, "temp_f", getName() + "." + label + ".temp", "temp");
				String humidity = processSensor(jo, "humidity", getName() + "." + label + ".humidity", "humidity");
				String pressure = processSensor(jo, "pressure", getName() + "." + label + ".pressure", "pressure");

				if (saveData) {
					sb = sb.append(lastPostA).append(",").append(rssi).append(",").append(pm2).append(",").append(temp)
							.append(",").append(humidity).append(",").append(pressure).append(",");
				}

				jo = (JSONObject) sensors.get(1);
				label = "B";
				Long lastPostB = jo.getLong("LastSeen");
				rssi = processSensor(jo, "RSSI", getName() + "." + label + ".RSSI", "rssi");
				pm2 = processSensor(jo, "PM2_5Value", getName() + "." + label + ".PM2.5", "cnt");
				temp = processSensor(jo, "temp_f", getName() + "." + label + ".temp", "temp");
				humidity = processSensor(jo, "humidity", getName() + "." + label + ".humidity", "humidity");
				pressure = processSensor(jo, "pressure", getName() + "." + label + ".pressure", "pressure");

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
						writer.write(getParmString(json, "SensorId"));
						writer.write(getParmString(json, "DateTime"));
						writer.write(getParmString(json, "Geo"));
						writer.write(getParmString(json, "Mem"));
						writer.write(getParmString(json, "Id"));
						writer.write(getParmString(json, "Adc"));
						writer.write(getParmString(json, "lat"));
						writer.write(getParmString(json, "lon"));
						// next 2 dropped in 3.00 version of sensor firmware
						writer.write(getParmString(json,"accuracy"));
						writer.write(getParmString(json,"elevation"));

						writer.write(getParmString(json, "version"));
						writer.write(getParmString(json, "uptime"));
						writer.write(getParmString(json, "rssi"));
						writer.write(getParmString(json, "hardwareversion"));
						writer.write(getParmString(json, "hardwarediscovered"));
						
						// next 4 dropped before 5/20/2019
						writer.write(getParmString(json, "current_temp_f"));
						writer.write(getParmString(json, "current_humidity"));
						writer.write(getParmString(json, "current_dewpoint_f"));
						writer.write(getParmString(json, "pressure"));

						// processSensorCnt also broadcasts the value
						writer.write(processSensorCnt(json, "pm1_0_atm_b"));
						writer.write(processSensorCnt(json, "pm2_5_atm_b"));
						writer.write(processSensorCnt(json, "pm10_0_atm_b"));
						writer.write(processSensorCnt(json, "pm1_0_cf_1_b"));
						writer.write(processSensorCnt(json, "pm2_5_cf_1_b"));
						writer.write(processSensorCnt(json, "pm10_0_cf_1_b"));
						writer.write(processSensorCnt(json, "p_0_3_um_b"));
						writer.write(processSensorCnt(json, "p_0_5_um_b"));
						writer.write(processSensorCnt(json, "p_1_0_um_b"));
						writer.write(processSensorCnt(json, "p_2_5_um_b"));
						writer.write(processSensorCnt(json, "p_5_0_um_b"));
						writer.write(processSensorCnt(json, "p_10_0_um_b"));
						writer.write(processSensorCnt(json, "pm1_0_atm"));
						writer.write(processSensorCnt(json, "pm2_5_atm"));
						writer.write(processSensorCnt(json, "pm10_0_atm"));
						writer.write(processSensorCnt(json, "pm1_0_cf_1"));
						writer.write(processSensorCnt(json, "pm2_5_cf_1"));
						writer.write(processSensorCnt(json, "pm10_0_cf_1"));
						writer.write(processSensorCnt(json, "p_0_3_um"));
						writer.write(processSensorCnt(json, "p_0_5_um"));
						writer.write(processSensorCnt(json, "p_1_0_um"));
						writer.write(processSensorCnt(json, "p_2_5_um"));
						writer.write(processSensorCnt(json, "p_5_0_um"));
						writer.write(processSensorCnt(json, "p_10_0_um"));
						writer.write(getParmString(json, "responseCode"));
						writer.write(getParmString(json, "responseCode_date"));
						writer.write(getParmString(json, "key1_responseCode"));
						writer.write(getParmString(json, "key1_responseCode_date"));
						writer.write(getParmString(json, "key1_count"));
						writer.write(getParmString(json, "key2_responseCode"));
						writer.write(getParmString(json, "key2_responseCode_date"));
						writer.write(getParmString(json, "key2_count"));
						writer.write(getParmString(json, "key1_responseCode_b"));
						writer.write(getParmString(json, "key1_responseCode_date_b"));
						writer.write(getParmString(json, "key1_count_b"));
						writer.write(getParmString(json, "key2_responseCode_b"));
						writer.write(getParmString(json, "key2_responseCode_date_b"));
						writer.write(getParmString(json, "key2_count_b"));
						writer.write("\n");
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
			// might be restarting so give a few seconds before trying again.
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// keeping compiler happy.
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
