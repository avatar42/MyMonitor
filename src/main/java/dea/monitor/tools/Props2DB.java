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
 * Tool for generating a database from the old style property files and working
 * with properties in the DB from the command line.
 * 
 * @author dea
 *
 */
public class Props2DB {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private ResourceBundle bundle;
	private DBInterface dbi;

	public Props2DB() throws SQLException {
		bundle = ResourceBundle.getBundle("checks");
		String dbPath = bundle.getString("db.path");
		dbi = new SQLiteDB(dbPath);
	}

	/**
	 * Replace all properties for each check listed in the checks.properties file.
	 */
	public void convert() {
		for (String key : bundle.keySet()) {
			if (key.startsWith("check.")) {
				String className = bundle.getString(key);
				String itemName = key.substring(6);
				dbi.clearItem(itemName);
				ResourceBundle itemBundle = ResourceBundle.getBundle(itemName);
				log.info(itemName + ":class:" + className);
				dbi.insertItemProperty(itemName, "class", className, true);
				for (String itemkey : itemBundle.keySet()) {
					dbi.insertItemProperty(itemName, itemkey, itemBundle.getString(itemkey), true);
				}
			}
		}

	}

	/**
	 * Replace all properties for itemName with the ones in the
	 * [itemName].properties file. And with the className passed.
	 * 
	 * @param itemName
	 * @param className to use for the "class" property
	 */
	public void addProps(String itemName, String className) {
		dbi.clearItem(itemName);
		ResourceBundle itemBundle = ResourceBundle.getBundle(itemName);
		log.info(itemName + ":class:" + className);
		dbi.insertItemProperty(itemName, "class", className, true);
		for (String itemkey : itemBundle.keySet()) {
			dbi.insertItemProperty(itemName, itemkey, itemBundle.getString(itemkey), true);
		}
	}

	/**
	 * Update property keyName for itemName with value and enabled set. Will add if
	 * unable to update.
	 * 
	 * @param itemName
	 * @param keyName
	 * @param value
	 * @param enabled
	 */
	public void updateProp(String itemName, String keyName, String value, boolean enabled) {
		if (dbi.updateItemProperty(itemName, keyName, value, enabled) == 0)
			dbi.insertItemProperty(itemName, keyName, value, true);
	}

	public static void usage() {
		System.err.println("USAGE: Props2DB all | -b name | -d name | -e name | name class | name key value");
		System.err.println("all = replace all the props in the DB");
		System.err.println("+b name = add broadcast.class to name's properties");
		System.err.println("-b name = disable broadcast.class to name's properties");
		System.err.println("-d name = disable all name's properties");
		System.err.println("-e name = enable all name's properties");
		System.err.println("name full.path.to.class = add a DB entry for name to use checker class");
		System.err.println("name key value = add a DB entry for name to use checker class");
	}

	public void parse(String[] args) {
		if (args == null || args.length == 0) {
			usage();
		} else if (args[0].equalsIgnoreCase("all")) {
			convert();
		} else if (args.length == 2) {
			if ("+b".equals(args[0])) {
				updateProp(args[1], "broadcast.class", "dea.monitor.broadcast.Homeseer", true);
			} else if ("-b".equals(args[0])) {
				updateProp(args[1], "broadcast.class", "dea.monitor.broadcast.Homeseer", false);
			} else if ("-d".equals(args[0])) {
				dbi.setEnabledItem(args[1], false);
			} else if ("-e".equals(args[0])) {
				dbi.setEnabledItem(args[1], true);
			} else {
				addProps(args[0], args[1]);
			}
		} else {
			usage();
		}

	}

	/**
	 * If args is all then wipes and adds all props names active in
	 * checks.properties Otherwise wants name and full class name
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Props2DB item;
		try {
			item = new Props2DB();
			item.parse(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
