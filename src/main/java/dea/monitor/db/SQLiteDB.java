package dea.monitor.db;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dea.monitor.broadcast.BroadcastInterface;

public class SQLiteDB implements DBInterface {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private static final String TABLE_ID = "items";
	private static final String COL_ID = "id";
	private static final String COL_NAME = "itemName";
	private static final String COL_KEY = "propName";
	private static final String COL_VAL = "propVal";
	private static final String COL_ACTIVE = "active";

	// SQL statement for creating a new table
	private static final String createItemsSql = "CREATE TABLE IF NOT EXISTS " + TABLE_ID + " (" + COL_ID
			+ " integer PRIMARY KEY AUTOINCREMENT," + " " + COL_NAME + " text NOT NULL, " + COL_KEY + " text NOT NULL, "
			+ COL_VAL + " text NOT NULL," + COL_ACTIVE + " integer DEFAULT 1);";

	private Connection conn;

	/**
	 * Get instance of class and open connection to the DB
	 * 
	 * @param dbPath path the DB file
	 * @throws SQLException if problem encountered getting a connection to the DB
	 */
	public SQLiteDB(String dbPath) throws SQLException {
		getConnection(dbPath);
	}

	/**
	 * Get instance of class and open connection to the DB
	 * 
	 * @param dbPath path the DB file
	 * @throws SQLException if problem encountered getting a connection to the DB
	 */
	public Connection getConnection(String dbPath) throws SQLException {
		if (conn == null) {
			File f = new File(dbPath);
			boolean isNew = !f.exists();

			String url = "jdbc:sqlite:" + f.getAbsolutePath();

			conn = DriverManager.getConnection(url);
			if (conn != null) {
				DatabaseMetaData meta = conn.getMetaData();
				log.info("The driver name is " + meta.getDriverName());
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

	/**
	 * Get a statement to use
	 */
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

		log.info("Removed " + rtn + " records");
		return rtn;
	}

	public int insertItemProperty(String name, String key, String val, boolean enabled) {
		int rtn = 0;
		String sql = "INSERT INTO " + TABLE_ID + "(" + COL_NAME + ", " + COL_KEY + ", " + COL_VAL + ", " + COL_ACTIVE
				+ ") VALUES(?, ?, ?, ?)";
		log.info("Adding item property:" + name + ":" + key + ":" + val);
		try (PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setString(1, name);
			stmt.setString(2, key);

			if (val == null)
				stmt.setNull(3, Types.VARCHAR);
			else
				stmt.setString(3, val);

			if (enabled) {
				stmt.setInt(4, 1);
			} else {
				stmt.setInt(4, 0);
			}
			rtn = stmt.executeUpdate();
		} catch (SQLException e) {
			log.error("Failed adding item property:" + name + ":" + key + ":" + val, e);
		}

		log.info("Inserted " + rtn + " records");
		return rtn;
	}

	public int setEnabledItem(String name, boolean isActive) {
		int rtn = 0;
		String sql = "UPDATE " + TABLE_ID + " SET " + COL_ACTIVE + "=? WHERE " + COL_NAME + "=?";

		log.info("Updating item properties active flag:" + name + ":" + isActive);
		try (PreparedStatement stmt = conn.prepareStatement(sql)) {
			if (isActive)
				stmt.setInt(1, 1);
			else
				stmt.setInt(1, 0);
			stmt.setString(2, name);

			rtn = stmt.executeUpdate();
		} catch (SQLException e) {
			log.error("Failed Updating item properties active flag:" + name + ":" + isActive, e);
		}

		if (!isActive) {
			BroadcastInterface broadcast;
			Map<String, String> camProps = getItemProperties(name);
			String clsStr = camProps.get("broadcast.class");

			try {
				if (clsStr != null) {
					Class<?> hiClass = Class.forName(clsStr);
					broadcast = (BroadcastInterface) hiClass.newInstance();
					if (broadcast != null) {
						int camBID = 0;
						if (!camProps.containsKey("broadcast.id")) {
							camBID = Integer.parseInt(camProps.get("broadcast.id"));
							broadcast.sendVal(camBID, 0f);
							broadcast.sendStatusString(camBID, "Disabled");
						}
					}
				}
			} catch (NumberFormatException | ClassNotFoundException | InstantiationException | IllegalAccessException
					| UnsupportedOperationException | IOException e) {
				log.error("Failed to mark remote device disabled.", e);
			}
		}
		log.info("Updated " + rtn + " records");
		return rtn;
	}

	public int updateItemProperty(String name, String key, String val, boolean enabled) {
		int rtn = 0;
		String sql = "UPDATE " + TABLE_ID + " SET " + COL_VAL + "=?, " + COL_ACTIVE + "=? WHERE " + COL_NAME + "=? AND "
				+ COL_KEY + "=?";

		log.info("Updating item property:" + name + ":" + key + ":" + val);
		try (PreparedStatement stmt = conn.prepareStatement(sql)) {
			if (val == null)
				stmt.setNull(1, Types.VARCHAR);
			else
				stmt.setString(1, val);

			if (enabled) {
				stmt.setInt(2, 1);
			} else {
				stmt.setInt(2, 0);
			}
			stmt.setString(3, name);
			stmt.setString(4, key);
			rtn = stmt.executeUpdate();
			if (rtn == 0) {
				rtn = insertItemProperty(name, key, val, enabled);
			}
		} catch (SQLException e) {
			log.error("Failed Updating item property:" + name + ":" + key + ":" + val, e);
		}

		log.info("Updated " + rtn + " records");
		return rtn;
	}

	public int renameItem(String oldName, String newName) {
		int rtn = 0;
		String sql = "UPDATE " + TABLE_ID + " SET " + COL_NAME + "=? WHERE " + COL_NAME + "=?";

		log.info("Renaming item:" + oldName + " to " + newName);
		try (PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setString(1, newName);
			stmt.setString(2, oldName);
			rtn = stmt.executeUpdate();
		} catch (SQLException e) {
			log.error("Failed Renaming item:" + oldName + " to " + newName, e);
		}

		log.info("Updated " + rtn + " records");
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

	public String getCheck(String bundleName) {
		String rtn = null;
		String sql = "SELECT " + COL_VAL + " FROM " + TABLE_ID + " WHERE " + COL_KEY + "='class'" + " AND " + COL_NAME
				+ "='" + bundleName + "'";
		log.info("Getting check:");
		try (PreparedStatement stmt = conn.prepareStatement(sql)) {
			ResultSet rs = stmt.executeQuery();
			// loop through the result set
			while (rs.next()) {
				rtn = rs.getString(COL_VAL);
			}
		} catch (SQLException e) {
			log.error("Failed getting check:", e);
		}

		return rtn;
	}

	public Map<String, String> getChecks(boolean includeDisabled) {
		Map<String, String> rtn = new HashMap<String, String>();
		String sql = "SELECT " + COL_NAME + "," + COL_VAL + " FROM " + TABLE_ID + " WHERE " + COL_KEY + "='class'";
		if (!includeDisabled) {
			sql = sql + " AND " + COL_ACTIVE + "=1";
		}
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

	public void close() {
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				log.error("Error closing connnection", e);
			}
		}

	}

	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}
}
