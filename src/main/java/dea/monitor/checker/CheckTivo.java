package dea.monitor.checker;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
	public static final String TOTAL_ITEMS_XPATH = "./TotalItems";
	public static final String ITEM_START_XPATH = "./ItemStart";
	public static final String ITEM_COUNT_XPATH = "./ItemCount";

	// relative to item
	public static final String IN_PROGRESS_XPATH = "./Details/InProgress";
	public static final String DURATION_XPATH = "./Details/Duration";
	public static final String SHOWING_DURATION_XPATH = "./Details/ShowingDuration";
	public static final String TITLE_XPATH = "./Details/Title";
	public static final String EPISODE_TITLE_XPATH = "./Details/EpisodeTitle";
	public static final String CAPTURE_DATE_XPATH = "./Details/CaptureDate";
	public static final String SHOWING_START_TIME_XPATH = "./Details/ShowingStartTime";
	public static final String STATION_XPATH = "./Details/SourceStation";
	public static final String CHANNEL_XPATH = "./Details/SourceChannel";
	public static final String SIZE_XPATH = "./Details/SourceSize";

	private int minSize = 0;
	private long maxShort = 300000;
	// it appears the report data can be up to 5 minutes out of date so add this
	// amount of allowable error.
	private long deviation = 5 * 60 * 1000;
	private boolean saveNPL = false;
	private String outPath;

	public void loadBundle() {
		broadcastType = getBundleVal(String.class, "broadcastType", "tivo");
		super.loadBundle();
		minSize = getBundleVal(Integer.class, "minSize", minSize);
		maxShort = getBundleVal(Long.class, "maxShort", maxShort);
		saveNPL = getBundleVal(Boolean.class, "saveNPL", saveNPL);
		outPath = getBundleVal(String.class, "exportPath", getName() + ".npl.csv");
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

	public String nowPlaying() throws NoSuchAlgorithmException, KeyManagementException, MalformedURLException,
			IOException, ProtocolException, ParseException {
		log.info("url:" + httpsURL);
		long startTime = System.currentTimeMillis();
		HttpURLConnection con = connect(httpsURL);
		String result = null;
		setRespCode(con.getResponseCode());
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

	private int getInt(XPath xpath, Node n, String expr) {
		String val = null;
		try {
			val = xpath.evaluate(expr, n);
			return Integer.parseInt(val);
		} catch (XPathExpressionException e) {
			log.error("Failed to find " + expr + " in item");
		} catch (NumberFormatException e) {
			log.error("Could not parse " + expr + " in item with value of:" + val);
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

	private String longToDateStr(Long l) {
		Date now = new Date(l);
		DateFormat formatter = new SimpleDateFormat("MM/dd/yy HH:mm:ss", Locale.US);
		return formatter.format(now);

	}

	public void run() {
		ignoreRespCode = true;
		running = true;
		log.info("reading url:" + httpsURL);
		int shortCnt = 0;
		int itemCnt = 50;
		FileWriter writer = null;
		for (int i = 0; i < retries; i++) {
			try {
				String xml = getUrl(httpsURL,false);
				if (respCode == HttpURLConnection.HTTP_OK && xml != null) { //
					// <TiVoContainer
					// xmlns="http://www.tivo.com/developer/calypso-protocol-1.6/">
					// <Details>
					// <ContentType>x-tivo-container/tivo-videos</ContentType>
					// <SourceFormat>x-tivo-container/tivo-dvr</SourceFormat>
					// <Title>Now Playing</Title>
					// <LastChangeDate>0x59965A3A</LastChangeDate>
					// <TotalItems>295</TotalItems>
					// <UniqueId>/NowPlaying</UniqueId>
					// </Details>
					// <SortOrder>Type,CaptureDate</SortOrder>
					// <GlobalSort>Yes</GlobalSort>
					// <ItemStart>50</ItemStart>
					// <ItemCount>50</ItemCount>
					// TODO: loop till read all items
					// https://10.10.4.53/TiVoConnect?Command=QueryContainer&Container=/NowPlaying&Recurse=Yes&ItemCount=50&AnchorOffset=50
					log.debug(xml);
					int readItems = 0;
					int totalItems = 50;
					if (saveNPL) {
						writer = new FileWriter(outPath);
						writer.write(
								"\"SHOW\",\"DATE\",\"SORTABLE DATE\",\"CHANNEL\",\"DURATION\",\"SIZE (GB)\",\"BITRATE (Mbps)\"\n");

					}
					StringBuilder errMsg = new StringBuilder();
					StringBuilder detailsSb = new StringBuilder();
					try {
						while (readItems < totalItems) {
							log.info("Getting " + readItems + " to " + (readItems + itemCnt) + " of " + totalItems);
							InputSource source = new InputSource(new StringReader(xml));

							DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
							DocumentBuilder db = dbf.newDocumentBuilder();
							Document document = db.parse(source);

							XPathFactory xpathFactory = XPathFactory.newInstance();
							XPath xpath = xpathFactory.newXPath();

							Node details = (Node) xpath.evaluate(REPORT_DETAILS_XPATH, document, XPathConstants.NODE);
							// LastChangeDate
							String reportDate = getValue(xpath, details, LAST_CHANGE_DATE_XPATH);
							long reportTime = Long.decode(reportDate) * 1000;
							// If saving get them all otherwise just what we got
							// first time.
							if (saveNPL) {
								totalItems = getInt(xpath, details, TOTAL_ITEMS_XPATH);
							} else {
								totalItems = getInt(xpath, document, ITEM_COUNT_XPATH);

							}
							if (totalItems == 0) {
								errMsg.append("No shows in my shows").append("\n");
								broadcastStatusCode = BC_CONTENT_MISSING;
							}
							NodeList itemList = (NodeList) xpath.evaluate(REPORT_ITEMS_XPATH, document,
									XPathConstants.NODESET);
							if (itemList.getLength() > 0) {
								for (int idx = 0; idx < itemList.getLength(); idx++) {
									Node item = itemList.item(idx);
									String title = getValue(xpath, item, TITLE_XPATH);
									String ep = getValue(xpath, item, EPISODE_TITLE_XPATH);
									if (ep != null) {
										title = title + " - " + ep;
									}
									// CaptureDate
									String cap = getValue(xpath, item, CAPTURE_DATE_XPATH);
									long captime = Long.decode(cap) * 1000;
									// StartTime
									String start = getValue(xpath, item, SHOWING_START_TIME_XPATH);
									long starttime = Long.decode(start) * 1000;

									long recordSecs = getLong(xpath, item, DURATION_XPATH);
									boolean inProgress = getBool(xpath, item, IN_PROGRESS_XPATH);

									long statTime;
									if (starttime > captime)
										statTime = starttime + recordSecs + deviation;
									else
										statTime = captime + recordSecs + deviation;

									String itemDets = title + ":Captured:" + new Date(captime) + ":Started:"
											+ new Date(starttime) + ":Recorded:" + (recordSecs / 1000) + " seconds :of:"
											+ ((reportTime - starttime) / 1000) + ":inProgress:" + inProgress;

									log.debug(itemDets);
									// if done recording check that amount
									// recorded
									// is close to what should have been.
									if (!inProgress) {
										long showSecs = getLong(xpath, item, SHOWING_DURATION_XPATH);
										// if recorded short more than
										// maxShort of
										// show run time
										if (recordSecs + maxShort < showSecs) {
											shortCnt++;
											detailsSb.append(title).append(" short ")
													.append((showSecs - recordSecs) / 1000).append(" seconds")
													.append("\n");
										}
									} else {
										log.debug(title + ":" + (reportTime / 1000) + ">" + (statTime / 1000) + " diff:"
												+ ((reportTime - statTime) / 1000));
										if (reportTime > statTime) {
											// channel you cannot get, get
											// added all
											// the time
											// Also if you have TWC they
											// crash my
											// tuner boxes at least once a
											// month
											// sending
											// out updates.
											if (recordSecs == 0) {
												detailsSb.append("Cable tuner needs reboot or channel not authorized:");
												detailsSb.append("Cable tuner needs reboot or channel not authorized:")
														.append("\n").append(itemDets);

											} else {
												detailsSb.append("Appears hung:").append("\n").append(itemDets);
												detailsSb.append("Appears hung:").append("\n").append(itemDets);
											}
										}
									}
									if (saveNPL) {
										float itemSize = getLong(xpath, item, SIZE_XPATH);
										// convert milisecs to fraction of day
										// for Excel time formating.
										float dayFraction = recordSecs / 86400000;
										writer.write("\"" + title + "\",\"" + longToDateStr(starttime) + "\",\""
												+ starttime + "\",\"" + getValue(xpath, item, CHANNEL_XPATH) + "="
												+ getValue(xpath, item, STATION_XPATH) + "\"," + (dayFraction) + ","
												+ (itemSize / 1073741824) + "," + (itemSize / recordSecs) + "\n");
									}
									readItems++;
								}
							}
							if (saveNPL) {
								xml = getUrl(new URL(httpsURL.toString() + itemCnt + "&AnchorOffset=" + readItems),false);
							}
						}
						if (shortCnt > 0) {
							errMsg.append(shortCnt).append(" shows are short\n");
							broadcastStatusCode = (float) shortCnt;
						}
						setStatusMsg("" + shortCnt + " shows are short");
						if (errMsg.length() > 0) {
							setErrStr(errMsg.toString());
							setDetails(detailsSb.toString());
						} else {
							setErrStr(null);
						}
						break;
					} catch (Exception e) {
						setErrStr("Caught exception parsing XML", e);
						setDetails(xml);
					} finally {
						if (writer != null) {
							writer.close();
						}
					}
				} else {
					if (xml != null) {
						setErrStr("respCode:" + respCode + " size:" + xml.length() + " < " + minSize);
					} else {
						setErrStr("respCode:" + respCode + " file empty");
						broadcastStatusCode = BC_CONTENT_MISSING;
					}
				}
			} catch (Exception e) {
				setErrStr("Exception reading xml", e);
				broadcastStatusCode = BC_EXCEPTION;
			}
		}
		setState(getErrStr() == null);
		broadcastStatus();
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
		item.cmd(args);
	}

}
