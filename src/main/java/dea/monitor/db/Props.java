package dea.monitor.db;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dea.monitor.tools.Props2DB;

/**
 * Class to abstract out the properties stuff so base code does not have to
 * think about if from props file or DB or if root bundle or sub bundle
 * 
 * @author avata
 *
 */
public class Props {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private ResourceBundle bundle;
	private Map<String, String> props;
	private String bundleName;
	private DBInterface dbi;

	public Props(String bundleName, DBInterface dbi, String className) throws InstantiationException {
		this.bundleName = bundleName;
		this.dbi = dbi;

		if (dbi == null) {
			bundle = ResourceBundle.getBundle(bundleName);
		} else {
			props = dbi.getItemProperties(bundleName);
			if (props.isEmpty()) {
				// if using DB but no props found then add to the DB
				try {
					Props2DB p2b = new Props2DB();
					p2b.addAllPropsToDB(bundleName, className);
					props = dbi.getItemProperties(bundleName);
				} catch (SQLException e) {
					throw new InstantiationException(e.getMessage());
				}
			}
		}
	}

	/**
	 * Convert string value to a class of type asClass
	 * 
	 * @param asClass
	 * @param stringVal a String of the word null is treated the same as null
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private <T> T stringToCls(Class<T> asClass, String stringVal) {

		if (stringVal != null) {
			if (Integer.class.isAssignableFrom(asClass)) {
				return (T) new Integer(stringVal);
			} else if (Long.class.isAssignableFrom(asClass)) {
				return (T) new Long(stringVal);
			} else if (Float.class.isAssignableFrom(asClass)) {
				return (T) new Float(stringVal);
			} else if (Boolean.class.isAssignableFrom(asClass)) {
				return (T) new Boolean(stringVal);
			} else if (String.class.isAssignableFrom(asClass)) {
				return (T) stringVal;
			} else if (ArrayList.class.isAssignableFrom(asClass)) {
				String tmp = stringVal;
				ArrayList<String> rtn = new ArrayList<String>();
				if (tmp != null) {
					StringTokenizer st = new StringTokenizer(tmp, ",");
					rtn.add(st.nextToken().trim());
				}
				return (T) rtn;
			} else {
				throw new UnsupportedClassVersionError("stringToCls does not support return type:" + asClass.getName());
			}
		}

		return null;
	}

	/**
	 * Update or adds property in DB and local Map copy
	 * 
	 * @param key
	 * @param newValue
	 * @return number of records changed
	 */
	public int updateProp(String key, Object newValue) {
		int cnt = 0;
		if (dbi != null && newValue != null) {
			cnt = dbi.updateItemProperty(bundleName, key, newValue.toString(), true);
			props.put(key, newValue.toString());
		}
		return cnt;
	}

	/**
	 * Get value from bundle or DB. Supported types are Integer, Long, Boolean or
	 * ArrayList<String> Note props files will used if available. If not the the DB
	 * version will be.
	 * 
	 * @param asClass      Class you want back
	 * @param key          to look for in the properties.
	 * @param defaultValue to return if not found
	 * @return property requested for the item
	 */
	public <T> T getBundleVal(Class<T> asClass, String key, T defaultValue) {
		T rtn = null;
		if (bundle != null && bundle.containsKey(key)) {
			try {
				rtn = stringToCls(asClass, bundle.getString(key));
			} catch (Exception e) {
				log.error("Failed to parse " + key + ":" + bundle.getString(key));
			}
		} else if (props != null && props.containsKey(key)) {
			try {
				rtn = stringToCls(asClass, props.get(key));
			} catch (Exception e) {
				log.error("Failed to parse " + key + ":" + props.get(key));
			}

		}
		if (rtn != null) {
			if ("null".equals(rtn)) {
				updateProp(key, defaultValue);
				return (T) defaultValue;
			} else {
				return rtn;
			}
		}

		log.warn("Using default value for " + key + ":" + defaultValue);
		// if not found then add default to DB is using DB.
		updateProp(key, defaultValue);

		return (T) defaultValue;
	}

	@Override
	public String toString() {
		return "Props [bundle=" + bundle + ", props=" + props + ", bundleName=" + bundleName + ", dbi=" + dbi + "]";
	}

}
