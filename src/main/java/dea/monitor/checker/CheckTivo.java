package dea.monitor.checker;

import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class CheckTivo extends CheckUrl {
	public static final String YES = "Yes";
	// absolute
	public static final String REPORT_DETAILS_XPATH = "/TiVoContainer/Details";
	public static final String REPORT_ITEMS_XPATH = "/TiVoContainer/Item";
	// relative to top
	public static final String LAST_CHANGE_DATE_XPATH = "./LastChangeDate";
	// relative to item
	public static final String IN_PROGRESS_XPATH = "./Details/InProgress";
	public static final String DURATION_XPATH = "./Details/Duration";
	public static final String SHOWING_DURATION_XPATH = "./Details/ShowingDuration";
	public static final String TITLE_XPATH = "./Details/Title";
	public static final String EPISODE_TITLE_XPATH = "./Details/EpisodeTitle";
	public static final String CAPTURE_DATE_XPATH = "./Details/CaptureDate";
	public static final String SHOWING_START_TIME_XPATH = "./Details/ShowingStartTime";

	private int minSize = 0;
	private long maxShort = 300000;
	// it appears the report data can be up to 5 minutes out of date so add this
	// amount of allowable error.
	private long deviation = 5 * 60 * 1000;

	public void loadBundle() {
		super.loadBundle();
		minSize = getBundleVal(Integer.class, "minSize", minSize);
		maxShort = getBundleVal(Long.class, "maxShort", maxShort);
	}

	public NodeList getNodeList(XPath xpath, Document doc, String expr) {
		try {
			XPathExpression pathExpr = xpath.compile(expr);
			return (NodeList) pathExpr.evaluate(doc, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String nowPlaying() throws NoSuchAlgorithmException,
			KeyManagementException, MalformedURLException, IOException,
			ProtocolException, ParseException {
		log.info("url:" + httpsURL);
		long startTime = System.currentTimeMillis();
		HttpURLConnection con = connect();
		String result = null;
		respCode = con.getResponseCode();
		// if connects OK get XML
		if (respCode == HttpURLConnection.HTTP_OK) {
			result = getUrlContentAsString(con);
		} else {
			setErrStr("Failed:" + respCode + ": " + con.getResponseMessage());
			log.warn(getErrStr());
		}
		timeToRun = System.currentTimeMillis() - startTime;
		return result;
	}

	// extracts the String value for the given expression
	private String getValue(XPath xpath, Node n, String expr) {
		try {

			return (String) xpath.evaluate(expr, n);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
		return null;
	}

	private long getLong(XPath xpath, Node n, String expr) {
		try {
			return Long.parseLong(xpath.evaluate(expr, n));
		} catch (XPathExpressionException e) {
			log.error("Failed to find " + expr + " in item");
		}
		return 0;
	}

	private boolean getBool(XPath xpath, Node n, String expr) {
		try {
			return YES.equals(xpath.evaluate(expr, n));
		} catch (XPathExpressionException e) {
			log.error("Failed to find " + expr + " in item");
		}
		return false;
	}

	public void run() {
		ignoreRespCode = true;
		running = true;
		log.info("reading url:" + httpsURL);
		for (int i = 0; i < retries; i++) {
			try {
				String xml = getUrl();
				if (respCode == HttpURLConnection.HTTP_OK && xml != null) { //
					log.debug(xml);
					try {
						InputSource source = new InputSource(new StringReader(
								xml));

						DocumentBuilderFactory dbf = DocumentBuilderFactory
								.newInstance();
						DocumentBuilder db = dbf.newDocumentBuilder();
						Document document = db.parse(source);

						XPathFactory xpathFactory = XPathFactory.newInstance();
						XPath xpath = xpathFactory.newXPath();

						Node details = (Node) xpath.evaluate(
								REPORT_DETAILS_XPATH, document,
								XPathConstants.NODE);
						// LastChangeDate
						String reportDate = getValue(xpath, details,
								LAST_CHANGE_DATE_XPATH);
						long reportTime = Long.decode(reportDate) * 1000;

						NodeList itemList = (NodeList) xpath.evaluate(
								REPORT_ITEMS_XPATH, document,
								XPathConstants.NODESET);
						StringBuilder errMsg = new StringBuilder();
						StringBuilder detailsSb = new StringBuilder();
						if (itemList.getLength() == 0) {
							errMsg.append("No shows in my shows").append("\n");
						} else {
							for (int idx = 0; idx < itemList.getLength(); idx++) {
								Node item = itemList.item(idx);
								String title = getValue(xpath, item,
										TITLE_XPATH);
								String ep = getValue(xpath, item,
										EPISODE_TITLE_XPATH);
								if (ep != null) {
									title = title + ":" + ep;
								}
								// CaptureDate
								String cap = getValue(xpath, item,
										CAPTURE_DATE_XPATH);
								long captime = Long.decode(cap) * 1000;
								// StartTime
								String start = getValue(xpath, item,
										SHOWING_START_TIME_XPATH);
								long starttime = Long.decode(start) * 1000;

								long recordSecs = getLong(xpath, item,
										DURATION_XPATH);
								boolean inProgress = getBool(xpath, item,
										IN_PROGRESS_XPATH);

								long statTime;
								if (starttime > captime)
									statTime = starttime + recordSecs
											+ deviation;
								else
									statTime = captime + recordSecs + deviation;

								String itemDets = title + ":Captured:"
										+ new Date(captime) + ":Started:"
										+ new Date(starttime) + ":Recorded:"
										+ (recordSecs / 1000) + " seconds :of:"
										+ ((reportTime - starttime) / 1000)
										+ ":inProgress:" + inProgress;

								log.debug(itemDets);
								// if done recording check that amount recorded
								// is close to what should have been.
								if (!inProgress) {
									long showSecs = getLong(xpath, item,
											SHOWING_DURATION_XPATH);
									// if recorded short more than maxShort of
									// show run time
									if (recordSecs + maxShort < showSecs) {
										errMsg.append(title)
												.append(" short ")
												.append((showSecs - recordSecs) / 1000)
												.append(" seconds")
												.append("\n");
									}
								} else {
									log.debug(title + ":" + (reportTime / 1000)
											+ ">" + (statTime / 1000)
											+ " diff:"
											+ ((reportTime - statTime) / 1000));
									if (reportTime > statTime) {
										// channel you cannot get, get added all
										// the time
										// Also if you have TWC they crash my
										// tuner boxes at least once a month
										// sending
										// out updates.
										if (recordSecs == 0) {
											detailsSb
													.append("Cable tuner needs reboot or channel not authorized:");
											detailsSb
													.append("Cable tuner needs reboot or channel not authorized:")
													.append("\n")
													.append(itemDets);

										} else {
											detailsSb.append("Appears hung:")
													.append("\n")
													.append(itemDets);
											detailsSb.append("Appears hung:")
													.append("\n")
													.append(itemDets);
										}
									}
								}
							}
						}
						if (errMsg.length() > 0)
							setErrStr(errMsg.toString());
						else
							setErrStr(null);
						break;
					} catch (Exception e) {
						setErrStr("Caught exception parsing XML", e);
						setDetails(xml);
					}
				} else {
					if (xml != null)
						setErrStr("respCode:" + respCode + " size:"
								+ xml.length() + " < " + minSize);
					else
						setErrStr("respCode:" + respCode + " file empty");
				}
			} catch (Exception e) {
				setErrStr("Exception reading xml", e);
			}
		}
		setState(getErrStr() == null);

		running = false;
		log.warn("read url");
	}

	@Override
	public String toString() {
		return "CheckTivo [" + super.toString() + "minSize=" + minSize + "]";
	}

	/**
	 * for testing
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		CheckTivo item = new CheckTivo();
		item.loadBundle(args[0]);
		Thread thread = item.background();

		while (thread.isAlive()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		item.log.info("done:" + item.getErrStr());
		item.log.info("end:" + item.toString());

	}

}
