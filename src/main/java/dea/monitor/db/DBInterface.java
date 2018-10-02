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

	int insertItemProperty(String name, String key, String val);

	int updateItemProperty(String name, String key, String val);

	Map<String, String> getItemProperties(String name);

	Map<String, String> getChecks();
}
