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
	public static final String IN_PROGRESS_XPATH = "./Details/InProgress";
	public static final String DURATION_XPATH = "./Details/Duration";
	public static final String SHOWING_DURATION_XPATH = "./Details/ShowingDuration";
	public static final String TITLE_XPATH = "./Details/Title";
	public static final String EPISODE_TITLE_XPATH = "./Details/EpisodeTitle";
	public static final String CAPTURE_DATE_XPATH = "./Details/CaptureDate";

	private int minSize = 0;
	private long maxShort = 300000;

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

						NodeList itemList = (NodeList) xpath.evaluate(
								"/TiVoContainer/Item", document,
								XPathConstants.NODESET);
						StringBuilder errMsg = new StringBuilder();
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
								String cap = getValue(xpath, item,
										CAPTURE_DATE_XPATH);
								long captime = Long.decode(cap) * 1000;
								// CaptureDate
								log.debug(title + ":Captured:"
										+ new Date(captime));
								long recordSecs = getLong(xpath, item,
										DURATION_XPATH);
								// if done recording
								if (!getBool(xpath, item, IN_PROGRESS_XPATH)) {
									long showSecs = getLong(xpath, item,
											SHOWING_DURATION_XPATH);
									// if recorded short more than 5 mins of
									// show
									if (recordSecs + maxShort < showSecs) {
										errMsg.append(title)
												.append(" short ")
												.append((showSecs - recordSecs) / 1000)
												.append(" seconds")
												.append("\n");
									}
								} else {
									if (System.currentTimeMillis() < captime
											+ recordSecs) {
										errMsg.append(title)
												.append(" appears hung")
												.append("\n");
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
