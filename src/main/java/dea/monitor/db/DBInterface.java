package dea.monitor.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

public interface DBInterface {

	/**
	 * Open a connection.
	 * 
	 * @param dbPath path to DB file
	 * @return Connection reference
	 * @throws SQLException
	 */
	Connection getConnection(String dbPath) throws SQLException;

	/**
	 * 
	 * @return
	 * @throws SQLException
	 */
	Statement getStatement() throws SQLException;

	/**
	 * Delete all the entries for check name.
	 * 
	 * @param name of item
	 * @return number of records removed
	 */
	int clearItem(String name);

	/**
	 * Add item property to DB
	 * 
	 * @param name
	 * @param key
	 * @param val
	 * @param enabled
	 * @return number of records changed
	 */
	int insertItemProperty(String name, String key, String val, boolean enabled);

	/**
	 * Update item property in DB
	 * 
	 * @param name
	 * @param key
	 * @param val
	 * @param enabled
	 * @return number of records changed
	 */
	int updateItemProperty(String name, String key, String val, boolean enabled);

	/**
	 * Rename checker set
	 * @param oldName
	 * @param newName
	 * @return number of records changed
	 */
	int renameItem(String oldName, String newName);
	
	/**
	 * Get all the properties for an item
	 * 
	 * @param name
	 * @return Map with props or empty Map is not found 
	 */
	Map<String, String> getItemProperties(String name);

	/**
	 * Get all the check names and classes indexed by name
	 * 
	 * @param includeDisabled if true include the items marked disabled.
	 * 
	 * @return
	 */
	Map<String, String> getChecks(boolean includeDisabled);

	/**
	 * Mark a check active / inactive
	 * 
	 * @param name
	 * @param isActive
	 */
	int setEnabledItem(String name, boolean isActive);

	/**
	 * Close the connect if still open.
	 */
	void close();
}
