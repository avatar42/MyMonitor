package dea.monitor.broadcast;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import twitter4j.JSONException;
import twitter4j.JSONObject;

/**
 * Interface for updating a remote system with status changes like a home
 * automation system
 * 
 * @author dea
 *
 */
public interface BroadcastInterface {
	/**
	 * Send test to a text to speech engine
	 * 
	 * @param text
	 * @throws IOException                   an error was encountered sending
	 * @throws UnsupportedOperationException if this class does not support this
	 */
	void speak(String text) throws IOException, UnsupportedOperationException;

	/**
	 * Set a remote object / device to the value either to display the status on a
	 * remote device of control it as in turning it off or on.
	 * 
	 * @param refID ID used to reference the device in this system.
	 * @param val   value to set it to
	 * @throws IOException                   an error was encountered sending
	 * @throws UnsupportedOperationException if this class does not support this
	 */
	void sendVal(Integer refID, Float val) throws IOException, UnsupportedOperationException;

	/**
	 * Set status string on a remote object / device.
	 * 
	 * @param refID  ID used to reference the device in this system.
	 * @param status value to set it to
	 * @throws IOException                   an error was encountered sending
	 * @throws UnsupportedOperationException if this class does not support this
	 */
	void sendStatusString(Integer refID, String status) throws IOException, UnsupportedOperationException;

	/**
	 * Set error message on a remote object / device.
	 * 
	 * @param refID  ID used to reference the device in this system.
	 * @param errMsg value to set it to
	 * @throws IOException                   an error was encountered sending
	 * @throws UnsupportedOperationException if this class does not support this
	 */
	void sendError(Integer refID, String errMsg) throws IOException, UnsupportedOperationException;

	/**
	 * Set details string on a remote object / device.
	 * 
	 * @param refID   ID used to reference the device in this system.
	 * @param details value to set it to
	 * @throws IOException                   an error was encountered sending
	 * @throws UnsupportedOperationException if this class does not support this
	 */
	void sendDetails(Integer refID, String details) throws IOException, UnsupportedOperationException;

	/**
	 * Set name on a remote object / device.
	 * 
	 * @param refID ID used to reference the device in this system.
	 * @param name  value to set it to
	 * @throws IOException                   an error was encountered sending
	 * @throws UnsupportedOperationException if this class does not support this
	 */
	void sendNameChg(Integer refID, String name) throws IOException, UnsupportedOperationException;

	/**
	 * Set region on a remote object / device.
	 * 
	 * @param refID  ID used to reference the device in this system.
	 * @param region value to set it to
	 * @throws IOException                   an error was encountered sending
	 * @throws UnsupportedOperationException if this class does not support this
	 */
	void sendRegionChg(Integer refID, String name) throws IOException, UnsupportedOperationException;

	/**
	 * Returns the value for a specific setting.
	 * 
	 * @param setting to get
	 * @return String of value
	 * @throws IOException                   an error was encountered sending
	 * @throws UnsupportedOperationException if this class does not support this
	 */
	String getSetting(String setting) throws IOException, UnsupportedOperationException;

	/**
	 * Get a Map<name,JSONObject> of all the MyMonitor devices the client knows
	 * 
	 * @return Map<name,JSONObject>
	 * @throws JSONException                 Malformed JSON was returned
	 * @throws IOException                   an error was encountered sending
	 * @throws UnsupportedOperationException if this class does not support this
	 */
	Map<String, JSONObject> getDevices() throws JSONException, IOException, UnsupportedOperationException;

	/**
	 * Get a Map<name,JSONObject> of all the MyMonitor devices the client knows for
	 * a region
	 * 
	 * @return Map<name,JSONObject>
	 * @throws JSONException                 Malformed JSON was returned
	 * @throws IOException                   an error was encountered sending
	 * @throws UnsupportedOperationException if this class does not support this
	 */
	Map<String, JSONObject> getDevicesByRegion(String region)
			throws JSONException, IOException, UnsupportedOperationException;

	/**
	 * Get a Set<name,ID> of all the MyMonitor regions the client knows
	 * 
	 * @return Set of region name Strings
	 * @throws JSONException                 Malformed JSON was returned
	 * @throws IOException                   an error was encountered sending
	 * @throws UnsupportedOperationException if this class does not support this
	 */
	Set<String> getRegions() throws JSONException, IOException, UnsupportedOperationException;

	/**
	 * Get device ref ID from remote system by name. Creates the device if needed
	 * and possible.
	 * 
	 * @param deviceName Name of the device in the remote system
	 * @return null if device not found or device ref ID
	 * @throws JSONException                 Malformed JSON was returned
	 * @throws IOException                   an error was encountered sending
	 * @throws UnsupportedOperationException if this class does not support this
	 */
	Integer getDeviceId(String deviceName) throws JSONException, IOException, UnsupportedOperationException;

	/**
	 * update or create a device on the remote system
	 * 
	 * @param refID      if 0 triggers a search by deviceName and then a create if
	 *                   not found.
	 * @param deviceName in remote system
	 * @param region     to use in remote system
	 * @return id of device in remote system. Same as refID if not new object.
	 */
	Integer updateDevice(Integer refID, String deviceName, String region) throws JSONException, IOException;
}
