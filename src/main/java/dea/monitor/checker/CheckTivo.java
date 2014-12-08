package dea.monitor.checker;

import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;

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
	private int minSize = 0;

	public void loadBundle() {
		super.loadBundle();
		minSize = Integer.parseInt(bundle.getString("minSize"));
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
			XPathExpression pathExpr = xpath.compile(expr);
			return (String) pathExpr.evaluate(n, XPathConstants.STRING);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void run() {
		ignoreRespCode = true;
		running = true;
		log.info("reading url:" + httpsURL);
		for (int i = 0; i < retries; i++) {
			try {
				String xml = getUrl();
				if (respCode == HttpURLConnection.HTTP_OK && xml != null) { //
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
						Node item = itemList.item(0);
						String status = xpath.evaluate("./Details/SourceSize",
								item);
						// TODO: add more checks
						// TODO: check that no duration is less than 30 mins
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
		item.loadBundle("HDtivoXL");
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
