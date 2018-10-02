package dea.monitor.tools;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dea.monitor.db.DBInterface;
import dea.monitor.db.SQLiteDB;

/**
 * Tool for generating a database from the old style property files.
 * 
 * @author dea
 *
 */
public class Props2DB {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private ResourceBundle bundle;

	public Props2DB() {
	}

	public void convert() {

		bundle = ResourceBundle.getBundle("checks");

		String dbPath = bundle.getString("db.path");

		DBInterface dbi;
		try {
			dbi = new SQLiteDB(dbPath);

			try (Connection conn = dbi.getConnection(dbPath); Statement stmt = dbi.getStatement()) {
				for (String key : bundle.keySet()) {
					if (key.startsWith("check.")) {
						String className = bundle.getString(key);
						String itemName = key.substring(6);
						dbi.clearItem(itemName);
						ResourceBundle itemBundle = ResourceBundle.getBundle(itemName);
						log.info(itemName + ":class:" + className);
						dbi.insertItemProperty(itemName, "class", className);
						for (String itemkey : itemBundle.keySet()) {
							dbi.insertItemProperty(itemName, itemkey, itemBundle.getString(itemkey));
						}
					}
				}

			} catch (SQLException e) {
				log.error("Failed to update DB", e);
			}
		} catch (SQLException e1) {
			log.error("Failed to connect to DB", e1);
		}

	}

	public static void main(String[] args) {
		Props2DB item = new Props2DB();
		try {
			item.convert();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
