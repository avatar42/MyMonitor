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
	 * @return either (1) the row count for SQL Data Manipulation Language (DML)
	 *         statements or (2) 0 for SQL statements that return nothing
	 */
	int clearItem(String name);

	/**
	 * Add item property to DB
	 * 
	 * @param name
	 * @param key
	 * @param val
	 * @param enabled
	 * @return rows changed
	 */
	int insertItemProperty(String name, String key, String val, boolean enabled);

	/**
	 * Update item property in DB
	 * 
	 * @param name
	 * @param key
	 * @param val
	 * @param enabled
	 * @return rows changed
	 */
	int updateItemProperty(String name, String key, String val, boolean enabled);

	/**
	 * Get all the properties for an item
	 * 
	 * @param name
	 * @return
	 */
	Map<String, String> getItemProperties(String name);

	/**
	 * Get all the check names and classes indexed by name
	 * 
	 * @param includeDisabled include the items marked disabled.
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
