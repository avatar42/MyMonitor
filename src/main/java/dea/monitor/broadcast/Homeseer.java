package dea.monitor.broadcast;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.commons.io.IOExceptionWithCause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dea.monitor.checker.CheckUrl;
import twitter4j.JSONArray;
import twitter4j.JSONException;
import twitter4j.JSONObject;

/**
 * Methods for talking to Homeseer See
 * https://homeseer.com/support/homeseer/HS3/A2ZLink/default.htm
 * 
 * May want to convert this or make a second version using this REST API to be
 * able to get devices by name instead of searching for the name in a list of
 * location located objects.
 * https://forums.homeseer.com/forum/developer-support/scripts-plug-ins-development-and-libraries/script-plug-in-library/77690-c-net-asp-net-hs3-z-wave-xml-http-restful-api
 * 
 * @author dea
 * 
 */
public class Homeseer extends CheckUrl implements BroadcastInterface {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	// aka Room
	private String location1;
	// aka Category or type
	private String location2;
	// default name for new objects
	private String defaultObjName = "MyMonitorNew";
	// group object creation events are in
	private String eventGroup;
	// base name of events to call to create new objects
	private String createEvent;

	public Homeseer() {
		loadBundle();
	}

	/********************************************
	 * Interface methods
	 ********************************************/

	public void speak(String text) throws IOException, UnsupportedOperationException {
		try {
			speak(text, null);
		} catch (Exception e) {
			throw new IOExceptionWithCause(e);
		}
	}

	public void sendVal(Integer refID, Float val) throws IOException, UnsupportedOperationException {
		try {
			controldevicebyvalue(refID, val);
		} catch (MalformedURLException | JSONException e) {
			throw new IOException(e);
		}
	}

	public void sendStatusString(Integer refID, String status) throws IOException, UnsupportedOperationException {
		try {
			setdeviceproperty(refID, "NewDevString", status);
		} catch (MalformedURLException | JSONException e) {
			throw new IOException(e);
		}
	}

	public void sendError(Integer refID, String errMsg) throws IOException, UnsupportedOperationException {
		try {
			setdeviceproperty(refID, "Attention", errMsg);
		} catch (MalformedURLException | JSONException e) {
			throw new IOException(e);
		}
	}

	/**
	 * UserNote is used in my Homeseer system for cat storage when a device is
	 * marked offline
	 */
	public void sendDetails(Integer refID, String details) throws IOException {
//		try {
//			String tmp = details;
//			if (tmp != null && tmp.length() > 140) {
//				tmp = details.substring(0, 140);
//			}
//			setdeviceproperty(refID, "UserNote", tmp);
//		} catch (MalformedURLException | JSONException e) {
//			throw new IOException(e);
//		}
	}

	public void sendNameChg(Integer refID, String name) throws IOException, UnsupportedOperationException {
		try {
			setdeviceproperty(refID, "Name", name);
		} catch (MalformedURLException | JSONException e) {
			throw new IOException(e);
		}
	}

	public void sendLocationChg(Integer refID, String name) throws IOException, UnsupportedOperationException {
		try {
			setdeviceproperty(refID, "Location", name);
		} catch (MalformedURLException | JSONException e) {
			throw new IOException(e);
		}
	}

	public void sendLocation2Chg(Integer refID, String name) throws IOException, UnsupportedOperationException {
		try {
			setdeviceproperty(refID, "Location2", name);
		} catch (MalformedURLException | JSONException e) {
			throw new IOException(e);
		}
	}

	public void sendAddressChg(Integer refID, String name) throws IOException, UnsupportedOperationException {
		try {
			setdeviceproperty(refID, "Address", name);
		} catch (MalformedURLException | JSONException e) {
			throw new IOException(e);
		}
	}

	public void sendTypeChg(Integer refID, String type) throws IOException, UnsupportedOperationException {
		try {
			setdeviceproperty(refID, "Device_Type_String", type);
		} catch (MalformedURLException | JSONException e) {
			throw new IOException(e);
		}
	}

	public Set<String> getRegions() throws JSONException, IOException {
		HashSet<String> set = new HashSet<String>();
		JSONObject obj = getlocations();
		JSONArray regions = obj.getJSONArray("location1");
		for (int i = 0; i < regions.length(); i++) {
			String region = regions.getString(i);
			set.add(region.toString());
		}
		return set;
	}

	public Map<String, JSONObject> getDevices() throws JSONException, IOException {
		return getstatus(null, null, location2);
	}

	public Map<String, JSONObject> getDevicesByRegion(String region) throws JSONException, IOException {
		return getstatus(null, region, location2);
	}

	public Integer getDeviceId(String deviceName) throws JSONException, IOException {
		Integer id = null;
		Map<String, JSONObject> map = getstatus(null, null, location2);
		if (map != null) {
			JSONObject obj = map.get(deviceName);
			id = obj.getInt("ref");
		}
		return id;
	}

	/**
	 * return Object of exists with passed attributes.
	 * 
	 * @param deviceName
	 * @param location1
	 * @param location2
	 * @return
	 */
	private JSONObject getObjByNameAndLocation(String deviceName, String location1, String location2) {
		JSONObject obj = null;
		Map<String, JSONObject> map = null;
		try {
			map = getstatus(null, location1, location2);
		} catch (JSONException | IOException e) {
			log.error("in getstatus(null, " + location1 + ", " + location2 + ");", e);
		}
		if (map != null) {
			obj = map.get(deviceName);
			if (obj != null) {
				log.info("Found remote device by name:" + deviceName + ", " + location1 + ", " + location2);
			}
		}

		return obj;
	}

	/**
	 * Tries various combos of location1 and location2 values in order of likelihood
	 * to make serious attempt to locate device before giving up. In order checks
	 * for device name in list found with location1,location2 pairs of:
	 * altLocation1, region; altLocation1, type; location1, region; location1, type;
	 * null, location2; location1, null; null, "offline"
	 * 
	 * @param deviceName
	 * @param altLocation1 if not null checks there first before trying default
	 *                     location1
	 * @param region       if not null checks there first before trying type for
	 *                     location2
	 * @param type         if not null checks there second before trying default
	 *                     location2
	 * @return JSONObject if found. null if not
	 */
	private JSONObject findObjWithOutID(String deviceName, String altLocation1, String region, String type) {
		JSONObject obj = null;
		if (altLocation1 != null) {
			if (region != null)
				obj = getObjByNameAndLocation(deviceName, altLocation1, region);
			if (obj == null && type != null) {
				obj = getObjByNameAndLocation(deviceName, altLocation1, type);
			}
		}
		if (obj == null) {
			if (region != null)
				obj = getObjByNameAndLocation(deviceName, location1, region);
			if (obj == null && type != null) {
				obj = getObjByNameAndLocation(deviceName, location1, type);
			}
		}
		if (obj == null) {
			obj = getObjByNameAndLocation(deviceName, null, location2);
		}

		if (obj == null) {
			obj = getObjByNameAndLocation(deviceName, location1, null);
		}
		// if running ChkSensors it might be in the offline group.
		if (obj == null) {
			obj = getObjByNameAndLocation(deviceName, null, "offline");
		}

		return obj;
	}

	private JSONObject createObj(String type) throws UnsupportedOperationException, IOException {
		JSONObject obj = null;

		// call script to create a new object of a type
		// Mine create with name=defaultObjName location1="MyMonitor"
		// location2=type
		runevent(eventGroup, createEvent + type);
		// give it a sec before trying to grab new device info or you might not see
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// ignored
		}
		// get new device
		obj = getObjByNameAndLocation(defaultObjName, location1, type);

		return obj;
	}

	/**
	 * Find matching object, creating it is need be and then update it.
	 * region = location1/room
	 * location2 = category
	 */
	public Integer updateDevice(Integer refID, String deviceName, String location1, String type, String address, String location2)
			throws JSONException, IOException {
		if (deviceName == null) {
			throw new IOException("deviceName is required");
		}
//		if (region == null) {
//			throw new IOException("region is required");
//		}

		JSONObject obj = null;
		if (refID == 0) {
			obj = findObjWithOutID(deviceName, location1, location2, type);
			if (obj != null) {
				refID = obj.getInt("ref");
			}
		} else {
			Map<String, JSONObject> map = getstatus(refID, null, null);
			if (map != null && !map.isEmpty()) {
				obj = map.values().iterator().next();
				if (obj != null) {
					log.info("Found remote device by refID");
				}
			}
		}

		// could not find by name or refID so add one.
		if (obj == null) {
			obj = createObj(type);
			if (obj != null) {
				refID = obj.getInt("ref");
			} else {
				throw new IOException("Unable to create new device");
			}
		}

		// update as needed
		if (refID > 0) {
			if (!deviceName.equals(obj.get("name")))
				sendNameChg(refID, deviceName);
			if (location1 != null && !location1.equals(obj.get("location")))
				sendLocationChg(refID, location1);
			if (location2 != null && !location2.equals(obj.get("location2")))
				sendLocation2Chg(refID, location2);
			String device_type_string = "MyMonitor-" + type;
			if (!device_type_string.equals(obj.get("device_type_string")))
				sendTypeChg(refID, device_type_string);

			// unfortunately there does not seem to be a way to read this from here.
			if (address != null)
				sendAddressChg(refID, address);
			log.info("Remote device updated");
		} else {
			throw new IOException("Unable to update device");
		}

		return refID;
	}

	/************************** helper / bridge methods ***************************/
	/**
	 * load configuration from properties file
	 */
	public void loadBundle() {
		bundle = ResourceBundle.getBundle("Homeseer");
		login = getBundleVal(String.class, "hs.user", login);
		password = getBundleVal(String.class, "hs.pass", password);
		location1 = getBundleVal(String.class, "hs.location1", location1);
		location2 = getBundleVal(String.class, "hs.location2", location2);
		eventGroup = getBundleVal(String.class, "hs.event.group", eventGroup);
		createEvent = getBundleVal(String.class, "hs.event.create", createEvent);
		defaultObjName = getBundleVal(String.class, "hs.default.object.name", defaultObjName);

		timeout = getBundleVal(Integer.class, "timeout", timeout);
	}

	/**
	 * Generate base URL from properties
	 * 
	 * @return URL string
	 */
	private String getBaseUrl() {
		if (bundle == null) {
			loadBundle();
		}
		StringBuilder url = new StringBuilder(getBundleVal(String.class, "httpsURL", null));
		url.append("user=").append(login).append("&pass=").append(password).append("&request=");

		return url.toString();
	}

	/**
	 * Call the built JSON API
	 * 
	 * @param httpsURL
	 * @return JSONObject
	 * @throws JSONException if malformed JSON reply was received
	 */
	public JSONObject getJson(URL httpsURL) throws JSONException {
		String s = getUrl(httpsURL, false);
		log.info(s);
		// Calls return JSON, "error" or some other error message
		// Often the JSON is just a simple { "Response":"ok" }
		// Note for example sending controldevicebyvalue a value of 1.1 when the device
		// has Pair.RangeStatusDecimals=0 on that status range will get a return of just
		// "error"
		if (s != null && s.indexOf('{') == 0) {
			JSONObject obj = new JSONObject(s);
			return obj;
		} else {
			throw new JSONException("Received:" + s);
		}
	}

	/***************** Homeseer API methods *******************************/

	/**
	 * loads device name to ref map
	 * 
	 * /JSON?request=getstatus&ref=##&location1=LOC1&location2=LOC2
	 * 
	 * @param ref=##         (only return the device that matches the specific
	 *                       reference #, this may be a list of reference #'s like
	 *                       3467,2342,869, omit or set to "all" to return all
	 *                       devices)
	 * @param location1=loc1 (only return the devices that are in the specific
	 *                       location1, omit or set to "all" for all devices at this
	 *                       location)
	 * @param location2=loc2 (only return the devices that are in the specific
	 *                       location2, omit or set to "all" for all devices at this
	 *                       location)
	 * 
	 * @return Map of JSONObjects containing device data keyed by name
	 * 
	 * @throws JSONException
	 * @throws IOException
	 */
	public Map<String, JSONObject> getstatus(Integer ref, String location1, String location2)
			throws JSONException, IOException {
		HashMap<String, JSONObject> map = new HashMap<String, JSONObject>();

		StringBuilder sb = new StringBuilder(getBaseUrl());
		sb.append("getstatus");
		if (ref != null) {
			sb.append("&ref=").append(URLEncoder.encode(ref.toString(), "Cp1252"));
		}
		if (location1 != null) {
			sb.append("&location1=").append(URLEncoder.encode(location1, "Cp1252"));
		}
		if (location2 != null) {
			sb.append("&location2=").append(URLEncoder.encode(location2, "Cp1252"));
		}
		JSONObject obj = getJson(new URL(sb.toString()));

		log.info(obj.toString());
		JSONArray devices = obj.getJSONArray("Devices");
		for (int i = 0; i < devices.length(); i++) {
			JSONObject device = devices.getJSONObject(i);
			map.put(device.getString("name"), device);
		}
		log.info(map.toString());

		return map;
	}

	/**
	 * 
	 * /JSON?request=getcontrol&ref=##
	 * 
	 * @param ref=### (where ### is the device reference #, or "all" to return
	 *                control information for all devices)
	 * 
	 */
	public void getcontrol(Integer ref) throws IOException, UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Control a device given the device's reference number "ref", and value
	 * "value". For example, if a light has a value of 0 for off, the following
	 * would turn off the device with reference # 3570:
	 * 
	 * /JSON?request=controldevicebyvalue&ref=3570&value=0
	 * 
	 * @param ref   device refID
	 * @param value to set
	 * @throws JSONException
	 * @throws IOException
	 */
	public void controldevicebyvalue(Integer ref, Float value) throws JSONException, IOException {
		StringBuilder sb = new StringBuilder(getBaseUrl());
		sb.append("controldevicebyvalue&ref=").append(ref);
		sb.append("&value=").append(value.toString());
		JSONObject obj = getJson(new URL(sb.toString()));

		log.debug(obj.toString());

	}

	/**
	 * Set the property a device given the device's reference number "ref", and
	 * value "value". For example, the following would set the status string to
	 * "test" of a virtual with reference # 35700:
	 * 
	 * JSON?request=setdeviceproperty&ref=3570&property=NewDevString&value=test
	 * 
	 * Note NewDevString is valid though not listed in the
	 * HomeSeerAPI.Enums.eDeviceProperty docs.
	 * 
	 * @param ref
	 * @param property is a HomeSeerAPI.Enums.eDeviceProperty Const name
	 * @param value    to set. null or empty string clears the value
	 * @throws JSONException
	 * @throws IOException
	 */
	public void setdeviceproperty(Integer ref, String property, String value) throws JSONException, IOException {
		StringBuilder sb = new StringBuilder(getBaseUrl());
		sb.append("setdeviceproperty&ref=").append(ref);
		sb.append("&property=").append(property);
		sb.append("&value=");
		if (value != null)
			sb.append(URLEncoder.encode(value, "Cp1252"));
		JSONObject obj = getJson(new URL(sb.toString()));

		log.debug(obj.toString());

	}

	/**
	 * performed such as controlling a light, a sequence of lights, a thermostat,
	 * etc. Events have two properties, a group name and an event name. This command
	 * returns the group name and event name for all events. These two pieces of
	 * information are used to control an event.
	 * 
	 * /JSON?request=getevents
	 * 
	 */
	public JSONArray getevents() throws IOException, UnsupportedOperationException {
		StringBuilder sb = new StringBuilder(getBaseUrl());
		sb.append("getevents");
		try {
			JSONObject obj = getJson(new URL(sb.toString()));

			log.debug(obj.toString());
			JSONArray events = obj.getJSONArray("Events");
			log.info(events.toString());
			return events;
		} catch (MalformedURLException | JSONException e) {
			throw new IOException(e);
		}
	}

	/**
	 * This command will execute the actions of an event. Pass the group name and
	 * event name. The group and name are not case sensitive.
	 * 
	 * Note: Only available if A2Z-Link is enabled for automation.
	 * /JSON?request=runevent&group=GROUPNAME&name=EVENTNAME
	 * 
	 * @param group
	 * @param name
	 */
	public void runevent(String group, String name) throws IOException, UnsupportedOperationException {
		StringBuilder sb = new StringBuilder(getBaseUrl());
		sb.append("runevent&group=").append(URLEncoder.encode(group, "Cp1252"));
		sb.append("&name=").append(URLEncoder.encode(name, "Cp1252"));
		try {
			JSONObject obj = getJson(new URL(sb.toString()));
			// On success you just get { "Response":"ok" }
			String resp = obj.toString();
			log.debug(resp);
			if (!resp.contains("ok")) {
				throw new IOException(resp);
			}
		} catch (MalformedURLException | JSONException e) {
			throw new IOException(e);
		}

	}

	/**
	 * /JSON?request=speak&phrase=text&host=HOST:NAME
	 * 
	 * @param text phrase to speak Says The [text up to last space] is now [last
	 *             word in text]
	 * @param host the speaker host to speak out of. HomeSeer supports multiple
	 *             hosts, like PC's and mobile devices. Each device is assgined a
	 *             unique host:name ID. For example, a host on the PC named
	 *             "hometroller" with the name "Android" would have the host name:
	 *             HomeTroller:Android. If this is added to the host parameter, then
	 *             the phrase will be spoken out that host only. Many hosts can be
	 *             added and are separated by a comma, IE:
	 *             host=HomeTroller:Android,iPhone:bill
	 * @throws JSONException
	 * @throws IOException
	 */
	public void speak(String text, String host) throws JSONException, IOException {
		StringBuilder sb = new StringBuilder(getBaseUrl());
		sb.append("speak&phrase=").append(URLEncoder.encode(text, "Cp1252"));
		if (host != null) {
			sb.append("&host=").append(URLEncoder.encode(host, "Cp1252"));
		}
		JSONObject obj = getJson(new URL(sb.toString()));

		log.debug(obj.toString());
	}

	/**
	 * Returns all the location names for location 1 and location 2
	 * 
	 * /JSON?request=getlocations
	 * 
	 * @throws JSONException
	 * @throws IOException
	 * 
	 */
	public JSONObject getlocations() throws JSONException, IOException {
		StringBuilder sb = new StringBuilder(getBaseUrl());
		sb.append("getlocations");

		JSONObject obj = getJson(new URL(sb.toString()));

		log.debug(obj.toString());

		return obj;

	}

	/**
	 * Returns the value for the given named counter
	 * 
	 * /JSON?request=getcounter&counter=NAME
	 * 
	 * @param counter
	 */
	public void getcounter(String counter) throws IOException, UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns the value for a specific setting. For example, the setting for the
	 * name of location 1 is called "gLocLabel". To get the name of this label use:
	 * 
	 * /JSON?request=getsetting&setting=gLocLabel
	 * 
	 * @param setting
	 */
	public String getSetting(String setting) throws IOException, UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/**
	 * for testing
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		BroadcastInterface item = new Homeseer();
		try {
			item.speak("Testing 1 2 3 done");
			item.sendVal(5430, 200f);
			System.out.println("\n getDevices:" + item.getDevices());
			System.out.println("\n getDevicesByRegion:" + item.getDevicesByRegion("MyMonitor"));
			System.out.println("\n getRegions:" + item.getRegions());
			System.out.println("\n update Obj:" + item.updateDevice(0, "dea42", "Hosting", "web", null, null));
//			Homeseer hs = new Homeseer();
//			hs.getevents();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
