package dea.monitor.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

public interface DBInterface {
	Connection getConnection(String dbPath) throws SQLException;

	Statement getStatement() throws SQLException;

	/**
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
	 * @return
	 */
	int insertItemProperty(String name, String key, String val);

	/**
	 * Update item property in DB
	 * 
	 * @param name
	 * @param key
	 * @param val
	 * @return
	 */
	int updateItemProperty(String name, String key, String val);

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
	 * @return
	 */
	Map<String, String> getChecks();

	/**
	 * Mark a check active / inactive
	 * 
	 * @param name
	 * @param isActive
	 */
	int setEnabledItem(String name, boolean isActive);
}
