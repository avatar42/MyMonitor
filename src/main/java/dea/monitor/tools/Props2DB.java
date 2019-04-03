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
	private String defaultBroadcastClass;

	public Props2DB() throws SQLException {
		try {
			bundle = ResourceBundle.getBundle("checks");
			String dbPath = bundle.getString("db.path");
			if (dbPath != null)
				dbi = new SQLiteDB(dbPath);
			defaultBroadcastClass = bundle.getString("default.broadcast.class");
		} catch (Exception e) {
			log.warn("No db.path in checks.properties file. Using propfiles instead");
		}
	}

	/**
	 * Replace all properties for each check listed in the checks.properties file.
	 */
	public int convertAll() {
		int cnt = 0;
		for (String key : bundle.keySet()) {
			if (key.startsWith("check.")) {
				String className = bundle.getString(key);
				String itemName = key.substring(6);
				cnt += addAllPropsToDB(itemName, className);
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
	public int addAllPropsToDB(String itemName) {
		String className = bundle.getString("check." + itemName);
		return addAllPropsToDB(itemName, className);
	}

	/**
	 * Replace all properties for itemName with the ones in the
	 * [itemName].properties file. And with the className passed.
	 * 
	 * @param itemName
	 * @param className to use for the "class" property
	 */
	public int addAllPropsToDB(String itemName, String className) {
		int cnt = 0;
		try {
			ResourceBundle itemBundle = ResourceBundle.getBundle(itemName);
			if (itemBundle != null && itemBundle.getString("region") != null) {
				cnt = dbi.clearItem(itemName);
				log.info(itemName + ":class:" + className);
				cnt += dbi.insertItemProperty(itemName, "class", className, true);
				for (String itemkey : itemBundle.keySet()) {
					cnt += dbi.insertItemProperty(itemName, itemkey, itemBundle.getString(itemkey), true);
				}
				cnt += updateProp(itemName, "broadcast.class", getDefaultBroadcastClass(), true);
			}
		} catch (Exception e) {
			// Failed to get props, skipping DB load form props
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

		int cnt = 0;
		if (value != null) {
			cnt = dbi.updateItemProperty(itemName, keyName, value, enabled);
			if (cnt == 0)
				cnt = dbi.insertItemProperty(itemName, keyName, value, true);
		}
		return cnt;
	}

	public static void usage(String className) {
		if (className == null)
			className = "dea.monitor.tools.Props2DB";

		System.err.println(
				"USAGE: " + className + " [-d db/path/file] all | +-b name | +-e name | name class | name key value");
		System.err.println("-d = use db/path/file for the path to the DB file instead of the one in checks.properties");
		System.err.println("all = replace all the props in the DB with those refereneced in checks.properties");
		System.err.println("name = overwrite name's properties in the DB with those from the properties file");
		System.err.println("With 2 arguments (other than -d dbPath)");
		System.err.println("+b name = add broadcast.class (default.broadcast.class) to name's properties");
		System.err.println("-b name = disable broadcast.class to name's properties");
		System.err.println("-e name = disable all name's properties");
		System.err.println("+e name = reenable all name's properties");
		System.err.println(
				"name full.qualified.className = add a DB entry for name to use checker class and replace all entries for name with those in name.properties");
		System.err.println("With 3 arguments (other than -d dbPath)");
		System.err.println("-r oldName newName = rename a check (monitor button name / remote object name)");
		System.err.println("name key value = add a DB entry for name of property key with value");
	}

	public String parse(String className, String[] args) {
		int cnt = 0;
		int firstArg = 0;
		String bundleName = null;
		if (args == null || args.length == 0) {
			usage(className);
		} else {
			try {
				if (args[0].equalsIgnoreCase("-d")) {
					// switch to using the DB passed on command line
					dbi = new SQLiteDB(args[1]);
					System.out.println("Using DB:" + args[1]);
					firstArg = 2;
				} else {
					System.out.println("Using DB:" + bundle.getString("db.path"));
				}

				if (args[firstArg].equalsIgnoreCase("all")) {
					cnt = convertAll();
				} else if (args.length == firstArg + 1) {
					String chkClassName = className;
					bundleName = args[firstArg];
					if (chkClassName == null) {
						try {
							chkClassName = bundle.getString("check." + bundleName);
						} catch (Exception e) {
							// props file not found. Try DB
						}
					}
					if (chkClassName == null && dbi != null)
						chkClassName = dbi.getCheck(bundleName);

					if (chkClassName != null) {
						cnt = addAllPropsToDB(bundleName, chkClassName);
					} else {
						System.err.println("Could not find checker class in props file or DB");
						usage(className);
					}

				} else if (args.length == firstArg + 2) {
					bundleName = args[firstArg + 1];
					if ("+b".equals(args[firstArg])) {
						cnt = updateProp(bundleName, "broadcast.class", getDefaultBroadcastClass(), true);
					} else if ("-b".equals(args[firstArg])) {
						cnt = updateProp(bundleName, "broadcast.class", getDefaultBroadcastClass(), false);
					} else if ("-e".equals(args[firstArg])) {
						cnt = dbi.setEnabledItem(bundleName, false);
					} else if ("+e".equals(args[firstArg])) {
						cnt = dbi.setEnabledItem(bundleName, true);
					}
					bundleName = args[firstArg];
					cnt = addAllPropsToDB(bundleName, args[firstArg + 1]);

				} else if (args.length == firstArg + 3) {
					if ("-r".equals(args[firstArg])) {
						cnt = dbi.renameItem(args[firstArg + 1], args[firstArg + 2]);
					} else {
						bundleName = args[firstArg];
						cnt = updateProp(bundleName, args[firstArg + 1], args[firstArg + 2], false);
					}
				} else {
					usage(className);
				}
			} catch (SQLException e) {
				System.err.println(e.getMessage());
				usage(className);
			}
		}
		System.out.println("Changed " + cnt + " records");
		return bundleName;
	}

	public ResourceBundle getBundle() {
		return bundle;
	}

	public DBInterface getDbi() {
		return dbi;
	}

	public String getDefaultBroadcastClass() {
		return defaultBroadcastClass;
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
			item.parse(null, args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
