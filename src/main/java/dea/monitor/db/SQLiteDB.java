package dea.monitor.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQLiteDB implements DBInterface {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private static final String TABLE_ID = "items";
	private static final String COL_ID = "id";
	private static final String COL_NAME = "itemName";
	private static final String COL_KEY = "propName";
	private static final String COL_VAL = "propVal";

	// SQL statement for creating a new table
	private static final String createItemsSql = "CREATE TABLE IF NOT EXISTS " + TABLE_ID + " (" + COL_ID
			+ " integer PRIMARY KEY AUTOINCREMENT," + " " + COL_NAME + " text NOT NULL, " + COL_KEY + " text NOT NULL, "
			+ COL_VAL + " text NOT NULL);";

	private Connection conn;

	public SQLiteDB(String dbPath) throws SQLException {
		getConnection(dbPath);
	}

	public Connection getConnection(String dbPath) throws SQLException {
		if (conn == null) {
			File f = new File(dbPath);
			boolean isNew = !f.exists();

			String url = "jdbc:sqlite:" + f.getAbsolutePath();

			conn = DriverManager.getConnection(url);
			if (conn != null) {
				DatabaseMetaData meta = conn.getMetaData();
				System.out.println("The driver name is " + meta.getDriverName());
				if (isNew)
					log.warn("A new database has been created.");
				try (Statement stmt = conn.createStatement()) {
					// create a new table
					stmt.executeUpdate(createItemsSql);
				} catch (SQLException e) {
					log.error("Failed getting connection:", e);
				}
			}
		}
		return conn;
	}

	public Statement getStatement() throws SQLException {
		return conn.createStatement();
	}

	public int clearItem(String name) {
		int rtn = 0;
		String sql = "DELETE FROM " + TABLE_ID + " where " + COL_NAME + "=?";
		log.info("Deleting item properties:" + name);
		try (PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setString(1, name);
			rtn = stmt.executeUpdate();
		} catch (SQLException e) {
			log.error("Failed adding item properties:" + name, e);
		}

		return rtn;
	}

	public int insertItemProperty(String name, String key, String val) {
		int rtn = 0;
		String sql = "INSERT INTO " + TABLE_ID + "(" + COL_NAME + ", " + COL_KEY + ", " + COL_VAL + ") VALUES(?, ?, ?)";
		log.info("Adding item property:" + name + ":" + key + ":" + val);
		try (PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setString(1, name);
			stmt.setString(2, key);
			stmt.setString(3, val);
			rtn = stmt.executeUpdate();
		} catch (SQLException e) {
			log.error("Failed adding item property:" + name + ":" + key + ":" + val, e);
		}

		return rtn;
	}

	public int updateItemProperty(String name, String key, String val) {
		int rtn = 0;
		String sql = "UPDATE  " + TABLE_ID + " SET " + COL_VAL + "=? WHERE " + COL_NAME + "=? AND " + COL_KEY + "=?";

		log.info("Updating item property:" + name + ":" + key + ":" + val);
		try (PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setString(1, val);
			stmt.setString(2, name);
			stmt.setString(3, key);
			rtn = stmt.executeUpdate();
		} catch (SQLException e) {
			log.error("Failed adding item property:" + name + ":" + key + ":" + val, e);
		}

		return rtn;
	}

	public Map<String, String> getItemProperties(String name) {
		Map<String, String> rtn = new HashMap<String, String>();
		String sql = "SELECT " + COL_KEY + "," + COL_VAL + " FROM " + TABLE_ID + " WHERE " + COL_NAME + "=?";
		log.info("Getting item properties:" + name);
		try (PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setString(1, name);
			ResultSet rs = stmt.executeQuery();

			// loop through the result set
			while (rs.next()) {
				rtn.put(rs.getString(COL_KEY), rs.getString(COL_VAL));
			}
		} catch (SQLException e) {
			log.error("Failed getting items properties:" + name, e);
		}

		return rtn;
	}

	public Map<String, String> getChecks() {
		Map<String, String> rtn = new HashMap<String, String>();
		String sql = "SELECT " + COL_NAME + "," + COL_VAL + " FROM " + TABLE_ID + " WHERE " + COL_KEY + "='class'";
		log.info("Getting checks:");
		try (PreparedStatement stmt = conn.prepareStatement(sql)) {
			ResultSet rs = stmt.executeQuery();

			// loop through the result set
			while (rs.next()) {
				rtn.put(rs.getString(COL_NAME), rs.getString(COL_VAL));
			}
		} catch (SQLException e) {
			log.error("Failed getting checks:", e);
		}

		return rtn;
	}

}
