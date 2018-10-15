package dea.monitor.tools;

import java.sql.SQLException;
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
	public int convert() {
		int cnt = 0;
		for (String key : bundle.keySet()) {
			if (key.startsWith("check.")) {
				String className = bundle.getString(key);
				String itemName = key.substring(6);
				cnt += dbi.clearItem(itemName);
				ResourceBundle itemBundle = ResourceBundle.getBundle(itemName);
				log.info(itemName + ":class:" + className);
				cnt += dbi.insertItemProperty(itemName, "class", className, true);
				for (String itemkey : itemBundle.keySet()) {
					cnt += dbi.insertItemProperty(itemName, itemkey, itemBundle.getString(itemkey), true);
				}
			}
		}
		return cnt;
	}

	/**
	 * Replace all properties for itemName with the ones in the
	 * [itemName].properties file. And with the className from the checks property
	 * file.
	 * 
	 * @param itemName
	 */
	public int addProps(String itemName) {
		String className = bundle.getString("check." + itemName);
		return addProps(itemName, className);
	}

	/**
	 * Replace all properties for itemName with the ones in the
	 * [itemName].properties file. And with the className passed.
	 * 
	 * @param itemName
	 * @param className to use for the "class" property
	 */
	public int addProps(String itemName, String className) {
		int cnt = dbi.clearItem(itemName);
		ResourceBundle itemBundle = ResourceBundle.getBundle(itemName);
		log.info(itemName + ":class:" + className);
		cnt += dbi.insertItemProperty(itemName, "class", className, true);
		for (String itemkey : itemBundle.keySet()) {
			cnt += dbi.insertItemProperty(itemName, itemkey, itemBundle.getString(itemkey), true);
		}

		return cnt;
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
	public int updateProp(String itemName, String keyName, String value, boolean enabled) {
		int cnt = dbi.updateItemProperty(itemName, keyName, value, enabled);
		if (cnt == 0)
			cnt = dbi.insertItemProperty(itemName, keyName, value, true);
		return cnt;
	}

	public static void usage() {
		System.err.println("USAGE: Props2DB all | +-b name | +-e name | name class | name key value");
		System.err.println("all = replace all the props in the DB");
		System.err.println("With 2 arguments");
		System.err.println("+b name = add broadcast.class (default.broadcast.class) to name's properties");
		System.err.println("-b name = disable broadcast.class to name's properties");
		System.err.println("-e name = disable all name's properties");
		System.err.println("+e name = reenable all name's properties");
		System.err.println("name full.path.to.class = add a DB entry for name to use checker class");
		System.err.println("With 3 arguments");
		System.err.println("-r oldName newName = rename a check (monitor button name / remote object name)");
		System.err.println("name key value = add a DB entry for name of property key with value");
	}

	public void parse(String[] args) {
		int cnt = 0;
		if (args == null || args.length == 0) {
			usage();
		} else if (args[0].equalsIgnoreCase("all")) {
			cnt = convert();
		} else if (args.length == 2) {
			if ("+b".equals(args[0])) {
				cnt = updateProp(args[1], "broadcast.class", bundle.getString("default.broadcast.class"), true);
			} else if ("-b".equals(args[0])) {
				cnt = updateProp(args[1], "broadcast.class", bundle.getString("default.broadcast.class"), false);
			} else if ("-e".equals(args[0])) {
				cnt = dbi.setEnabledItem(args[1], false);
			} else if ("+e".equals(args[0])) {
				cnt = dbi.setEnabledItem(args[1], true);
			} else {
				cnt = addProps(args[0], args[1]);
			}
		} else if (args.length == 3) {
			if ("-r".equals(args[0])) {
				cnt = dbi.renameItem(args[1], args[2]);
			} else {
				cnt = updateProp(args[0], args[1], args[2], false);
			}
		} else {
			usage();
		}

		System.out.println("Changed " + cnt + " records");
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
