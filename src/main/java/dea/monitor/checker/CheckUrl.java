package dea.monitor.checker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;

import dea.monitor.tools.HttpsVerifier;
import dea.monitor.tools.Utils;

/**
 * Check a URL to make sure it is readable. Needs keystore in working dir
 * keytool -genkey -alias dea42.com -keyalg RSA -keystore keystorefile.store
 * -keysize 2048
 * 
 * @author dea
 * 
 */
public class CheckUrl extends CheckBase {
	public static final String URL_ENCODING = "utf-8";
	public static final String WWW_AUTHENTICATE = "WWW-Authenticate";
	public static final String AUTH_BASIC = "basic";
	public static final String AUTH_DIGEST = "digest";
	public static final String OWL_CONTENT_TYPE = "unknown/unknown";

	public static final SimpleDateFormat fileDateTimeFmt = new SimpleDateFormat(
			"yyMMdd@HHmmss");

	// setable options
	protected int retries = 2;
	protected URL httpsURL;
	protected URL loginURL;
	protected String loginNameParam;
	protected String loginPasswordParam;
	protected String loginParam;
	protected String sessionKeyParam;

	protected String keyFile = "keystorefile.store";
	protected String keyPass = "password";
	protected String login;
	protected String password;
	protected String authType = "Basic";
	protected int timeout = 15000; // in milisecs
	protected String checkString = null;
	protected String failString = null;
	protected Pattern pattern = null;
	protected Boolean followRedirects = false;
	protected boolean saveImage = false;
	protected String savePath = ".";

	// checkable return values
	protected Date modDate = null;
	protected long len = 0;
	protected int respCode = 0;
	protected long timeToRun = 0;
	protected boolean foundString = false;
	protected boolean ignoreRespCode = false;
	private Map<String, List<String>> conHeaders;
	private Header[] respHeaders;
	protected String sessionId;
	protected String urlMethod = HttpGet.METHOD_NAME;

	// globals
	protected SSLSocketFactory sslSocketFactory;
	protected CloseableHttpClient httpclient;
	// Create a local instance of cookie store
	protected CookieStore cookieStore;

	protected SimpleDateFormat headerDateFormatFull = new SimpleDateFormat(
			"EEE, dd MMM yyyy HH:mm:ss zzz");
	protected SimpleDateFormat headerDateFormat = new SimpleDateFormat(
			"dd MMM yyyy HH:mm:ss zzz");

	protected String userAgentString = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.101 Safari/537.36";
	protected HttpContext context = new BasicHttpContext();

	public CheckUrl() {
		// make sure cookies is turn on
		CookieManager ckman = new CookieManager();
		ckman.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
		CookieHandler.setDefault(ckman);
		cookieStore = ckman.getCookieStore();
	}

	public void setKeystore() {
		if (System.getProperty("javax.net.ssl.trustStorePassword") == null) {

			File f = new File(keyFile);

			// Keystore file
			System.setProperty("javax.net.ssl.keyStore", f.getAbsolutePath());
			System.setProperty("javax.net.ssl.keyStorePassword", keyPass);
			// TrustStore file assumed same
			System.setProperty("javax.net.ssl.trustStore", f.getAbsolutePath());
			System.setProperty("javax.net.ssl.trustStorePassword", keyPass);
		}
	}

	protected void shutdownClient() {
		if (httpclient != null) {
			try {
				httpclient.close();
			} catch (IOException e) {
				log.error("Client close failed:", e);
			}
		}
	}

	/**
	 * Get the first (usually only) header value for a given key
	 * 
	 * @param asClass
	 *            Class type to return;
	 * @param headers
	 *            returned from URL
	 * @param key
	 *            to look for
	 * @param defaultVal
	 *            value to return if key not found.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected <T> T getFirstHeader(Class<T> asClass,
			Map<String, List<String>> headers, String key, Object defaultVal) {
		List<String> lms = headers.get(key);
		if (lms != null && !lms.isEmpty()) {
			try {
				if (Date.class.isAssignableFrom(asClass))
					return (T) parseDate(lms.get(0));

				if (Integer.class.isAssignableFrom(asClass))
					return (T) new Integer(lms.get(0));

				if (String.class.isAssignableFrom(asClass))
					return (T) lms.get(0);

			} catch (Exception e) {
				log.error("Failed to parse " + key + ":" + lms.get(0));
			}
		}

		return (T) defaultVal;
	}

	/**
	 * Read the contents of the url into a String
	 * 
	 * @param con
	 *            open connection
	 * @return contents of page
	 */
	protected String getUrlContentAsString(HttpURLConnection con) {
		StringBuilder sb = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				con.getInputStream()))) {

			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
		} catch (Exception e) {
			setErrStr("Failed reading url", e);
		} finally {

		}

		return sb.toString();
	}

	/**
	 * Get Last-Modified and Content-Length from headers. Prints headers at info
	 * log level
	 */
	protected void checkHeaders(HttpURLConnection con) throws ParseException {
		conHeaders = con.getHeaderFields();
		for (String key : conHeaders.keySet()) {
			log.info(key + ":" + conHeaders.get(key));
		}
		modDate = getFirstHeader(Date.class, conHeaders, "Last-Modified",
				modDate);
		len = getFirstHeader(Long.class, conHeaders, "Content-Length", len);
		contentType = getFirstHeader(String.class, conHeaders, "Content-Type",
				contentType);

	}

	private Date parseDate(String s) {
		Date modDate = null;
		try {
			modDate = headerDateFormatFull.parse(s);
		} catch (ParseException e) {
			// parse without day of week since some misspell them
			int i = s.indexOf(",");
			if (i > -1) {
				s = s.substring(i);
			}
			try {
				modDate = headerDateFormat.parse(s.trim());
			} catch (Exception e1) {
				log.warn("Exception parsing:" + s);
			}
		}

		return modDate;
	}

	/**
	 * Prints headers at info log level
	 */
	protected void checkHeaders(HttpUriRequest request) throws ParseException {
		log.info("HttpUriRequest:");
		respHeaders = request.getAllHeaders();
		for (Header header : respHeaders) {
			log.info(header.getName() + ":" + header.getValue());
		}
		log.info("Cookies:" + getCookies(request.getURI()));
	}

	/**
	 * Get Last-Modified and Content-Length from headers. Prints headers at info
	 * log level
	 */
	protected void checkHeaders(HttpResponse response, URI uri)
			throws ParseException {
		log.info("HttpResponse:");
		if (response != null) {
			respHeaders = response.getAllHeaders();
			for (Header header : respHeaders) {
				log.info(header.getName() + ":" + header.getValue());
				if (header.getName().equalsIgnoreCase("Last-Modified")) {
					modDate = parseDate(header.getValue());
				} else if (header.getName().equalsIgnoreCase("Content-Length")) {
					len = new Long(header.getValue());
				} else if (header.getName().equalsIgnoreCase("Content-Type")) {
					contentType = header.getValue();
				} else if (header.getName().equalsIgnoreCase(sessionKeyParam)) {
					sessionId = header.getValue();
				} else if (header.getName().equalsIgnoreCase("Date")) {
					modDate = parseDate(header.getValue());
				} else if (header.getName().equalsIgnoreCase("Location")) {
					// if (respCode == HttpURLConnection.HTTP_MOVED_TEMP) {
					// URL oldUrl = httpsURL;
					// try {
					// log.info("Changing URL:" + header.getValue());
					// httpsURL = new URL(header.getValue());
					// } catch (MalformedURLException e) {
					// log.error("Invaild redirect:" + header.getValue());
					// httpsURL = oldUrl;
					// }
					// }
				}
			}
		}
		setCookies(response, uri);
		log.info(getCookies(uri));
	}

	protected boolean isHttps(URL url) {
		return (url != null && url.getProtocol().equals("https"));
	}

	private void initSSL() {
		initSSL(httpsURL);
	}

	private void initSSL(URL url) {
		if (isHttps(url)) {
			log.info("Adding SSL settings");
			// setKeystore();
			HttpsVerifier.addHost(url.getHost());

			HttpsURLConnection.setDefaultHostnameVerifier(HttpsVerifier
					.getInstance());

			// since most of the stuff we hit is self signed or expired just
			// trust them
			try {
				final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
					public void checkClientTrusted(
							final X509Certificate[] chain, final String authType)
							throws CertificateException {
					}

					public void checkServerTrusted(
							final X509Certificate[] chain, final String authType)
							throws CertificateException {
					}

					public X509Certificate[] getAcceptedIssuers() {
						return null;
					}

				} };

				final SSLContext sslContext = SSLContext.getInstance("SSL");
				sslContext.init(null, trustAllCerts,
						new java.security.SecureRandom());
				sslSocketFactory = sslContext.getSocketFactory();
				Authenticator.setDefault(new Authenticator() {
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(login, password
								.toCharArray());
					}
				});
			} catch (KeyManagementException | NoSuchAlgorithmException e) {
				setErrStr("Failed reading url", e);
			}
		}
	}

	protected void tryTlsPlusCertInstall(HttpURLConnection con, Exception e)
			throws KeyManagementException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException, IOException {
		if (httpsURL.getProtocol().equals("https") && keyFile != null) {
			log.error("Trying to install cert as last final option", e);
			int port = httpsURL.getPort();
			if (port == -1)
				port = httpsURL.getDefaultPort();

			// SSLSocketFactory sf = install(httpsURL.getHost(), port, keyPass,
			// keyFile);
			char[] passphrase = keyPass.toCharArray();

			File file = new File(keyFile);
			if (file.isFile() == false) {
				char SEP = File.separatorChar;
				File dir = new File(System.getProperty("java.home") + SEP
						+ "lib" + SEP + "security");
				file = new File(dir, keyFile);
				if (file.isFile() == false) {
					file = new File(dir, "cacerts");
				}
			}
			log.info("Loading KeyStore " + file + "...");
			InputStream in = new FileInputStream(file);
			KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
			try {
				ks.load(in, passphrase);
			} catch (Exception e1) {
				log.error(keyPass, e1);
			}
			in.close();

			SSLContext context = SSLContext.getInstance("TLS");
			TrustManagerFactory tmf = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(ks);
			X509TrustManager defaultTrustManager = (X509TrustManager) tmf
					.getTrustManagers()[0];
			SavingTrustManager tm = new SavingTrustManager(defaultTrustManager);
			context.init(null, new TrustManager[] { tm }, null);
			SSLSocketFactory factory = context.getSocketFactory();

			log.info("Opening connection to " + httpsURL.getHost() + ":" + port
					+ "...");
			SSLSocket socket = (SSLSocket) factory.createSocket(
					httpsURL.getHost(), port);
			socket.setSoTimeout(10000);
			try {
				log.info("Starting SSL handshake...");
				socket.startHandshake();
				socket.close();
				log.info("");
				log.info("No errors, certificate is already trusted");
			} catch (SSLException e1) {
				log.error("", e1);

				X509Certificate[] chain = tm.chain;
				if (chain == null) {
					log.info("Could not obtain server certificate chain");
					factory = null;
				} else {

					log.info("");
					log.info("Server sent " + chain.length + " certificate(s):");
					log.info("");
					MessageDigest sha1 = MessageDigest.getInstance("SHA1");
					MessageDigest md5 = MessageDigest.getInstance("MD5");
					for (int i = 0; i < chain.length; i++) {
						X509Certificate cert = chain[i];
						log.info(" " + (i + 1) + " Subject "
								+ cert.getSubjectDN());
						log.info("   Issuer  " + cert.getIssuerDN());
						sha1.update(cert.getEncoded());
						log.info("   sha1    "
								+ Utils.toHexString(sha1.digest()));
						md5.update(cert.getEncoded());
						log.info("   md5     "
								+ Utils.toHexString(md5.digest()));
						log.info("");
					}
					int k = 0;
					X509Certificate cert = chain[k];
					String alias = httpsURL.getHost() + "-" + (k + 1);
					ks.setCertificateEntry(alias, cert);

					OutputStream out = new FileOutputStream(keyFile);
					ks.store(out, passphrase);
					out.close();

					log.info("");
					log.info(cert.toString());
					log.info("");
					log.info("Added certificate to keystore '" + keyFile
							+ "' using alias '" + alias + "'");
				}
			}

			if (factory != null)
				HttpsURLConnection.setDefaultSSLSocketFactory(factory);

			respCode = con.getResponseCode();
		} else {
			log.error("Failed to connect", e);
		}

	}

	protected HttpURLConnection connect() throws IOException {
		initSSL();

		log.info("Connecting to:" + httpsURL);
		HttpURLConnection.setFollowRedirects(followRedirects);
		HttpURLConnection con = (HttpURLConnection) httpsURL.openConnection();
		con.setConnectTimeout(timeout);
		if (con instanceof HttpsURLConnection) {
			((HttpsURLConnection) con).setHostnameVerifier(HttpsVerifier
					.getInstance());
			((HttpsURLConnection) con).setSSLSocketFactory(sslSocketFactory);
		}
		con.setRequestMethod("GET");

		// con.setRequestProperty(
		// "User-Agent",
		// userAgentString);

		if (login != null) {
			con.setRequestProperty("Authorization", getBasicAuth());
		}
		return con;
	}

	/**
	 * Executes a request using the default context.
	 * 
	 * @param request
	 *            - the request to execute
	 * @param context
	 *            TODO
	 * @return - the response to this request
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws ParseException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws KeyManagementException
	 */
	protected HttpResponse execute(HttpUriRequest request, HttpContext context)
			throws ClientProtocolException, IOException, ParseException,
			NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
		HttpResponse response = null;
		if (isHttps(request.getURI().toURL())) {
			// Trust own CA and all self-signed certs
			final SSLContext sslcontext = SSLContext.getDefault();
			X509HostnameVerifier allowAllVerifer = new X509HostnameVerifier() {

				@Override
				public boolean verify(String hostname, SSLSession session) {
					log.info("verify(String hostname,SSLSession session)");
					return true;
				}

				@Override
				public void verify(String host, SSLSocket ssl)
						throws IOException {
					log.info("verify(String host, SSLSocket ssl)");
				}

				@Override
				public void verify(String host, X509Certificate cert)
						throws SSLException {
					log.info("verify(String host, X509Certificate cert)");
				}

				@Override
				public void verify(String host, String[] cns,
						String[] subjectAlts) throws SSLException {
					log.info("verify(String host, String[] cns,\r\n"
							+ "						String[] subjectAlts)");
				}
			};
			final SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
					sslcontext, allowAllVerifer);
			httpclient = HttpClients
					.custom()
					.setUserAgent(userAgentString)
					.setHostnameVerifier(allowAllVerifer)
					.setSSLSocketFactory(sslsf)
					.setSslcontext(
							new SSLContextBuilder().loadTrustMaterial(
									KeyStore.getInstance(KeyStore
											.getDefaultType()),
									new TrustStrategy() {
										public boolean isTrusted(
												X509Certificate[] arg0,
												String arg1)
												throws CertificateException {
											log.info("isTrusted(X509Certificate[] arg0,String arg1)");
											return true;
										}
									}).build()).build();
		} else {
			httpclient = HttpClients.custom().setUserAgent(userAgentString)
					.build();
			// .setDefaultCookieStore(cookieStore)
		}
		log.info("Doing " + request.getMethod() + " to " + request.getURI());
		checkHeaders(request);
		response = httpclient.execute(request); // , context);
		checkHeaders(response, request.getURI());
		respCode = response.getStatusLine().getStatusCode();
		return response;
	}

	private Header getAuthHeader(HttpResponse response) {
		Header solution = null;
		try {
			// Get the challenge.
			final Header challenge = response.getHeaders(WWW_AUTHENTICATE)[0];
			String cVal = challenge.getValue().toLowerCase();
			if (cVal.contains(AUTH_BASIC)) {
				final BasicScheme md5Auth = new BasicScheme();
				// Solve it.
				md5Auth.processChallenge(challenge);
				solution = md5Auth.authenticate(
						new UsernamePasswordCredentials(login, password),
						new BasicHttpRequest(HttpGet.METHOD_NAME, httpsURL
								.getPath()), context);

			} else if (cVal.contains(AUTH_DIGEST)) {
				// A org.apache.http.impl.auth.DigestScheme instance is
				// what will process the challenge from the web-server
				final DigestScheme md5Auth = new DigestScheme();
				// Solve it.
				md5Auth.processChallenge(challenge);

				// Generate a solution Authentication header using your
				// username and password.
				solution = md5Auth.authenticate(
						new UsernamePasswordCredentials(login, password),
						new BasicHttpRequest(HttpGet.METHOD_NAME, httpsURL
								.getPath()), context);
				log.info("auth header:" + solution.getName() + ":"
						+ solution.getValue());
			} else {
				log.error("Need way to handle auth for:" + challenge.getValue());
			}
		} catch (MalformedChallengeException | AuthenticationException e) {
			setErrStr("Failed creating auth header", e);
		}

		return solution;
	}

	/**
	 * Post login and save session ID if given in response
	 * 
	 * @return HttpResponse
	 */
	protected String login() {
		HttpResponse response = null;
		String responseStr = null;
		if (loginURL != null) {
			initSSL(loginURL);
			try {
				HttpPost request = new HttpPost(new URI(loginURL.toString()));
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
				if (loginParam != null) {
					BasicNameValuePair vp = new BasicNameValuePair(
							loginNameParam, login);
					nameValuePairs.add(vp);
					log.info("adding login parm:" + vp);
				}
				if (loginPasswordParam != null) {
					BasicNameValuePair vp = new BasicNameValuePair(
							loginPasswordParam, password);
					nameValuePairs.add(vp);
					log.info("adding login parm:" + vp);
				}
				if (loginParam != null) {
					StringTokenizer parms = new StringTokenizer(loginParam, "&");
					while (parms.hasMoreTokens()) {
						String pair = parms.nextToken();
						int idx = pair.indexOf('=');
						BasicNameValuePair vp = new BasicNameValuePair(
								pair.substring(0, idx), pair.substring(idx + 1));
						nameValuePairs.add(vp);
						log.info("adding login parm:" + vp);
					}
				}
				request.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				response = execute((HttpUriRequest) request, context);
				log.info("login response code:" + respCode);
				if (respCode == HttpStatus.SC_OK
						|| respCode == HttpStatus.SC_MOVED_TEMPORARILY) {
					HttpEntity entity = response.getEntity();
					if (entity != null) {
						if (contentType.contains("text")) {
							responseStr = EntityUtils.toString(entity);
							log.info("responseStr:" + responseStr);
						}
					}
				}
			} catch (URISyntaxException | IOException | ParseException
					| NoSuchAlgorithmException | KeyManagementException
					| KeyStoreException e) {
				setErrStr("Failed posting URL", e);
			} finally {
				shutdownClient();
			}
		}
		return responseStr;
	}

	protected HttpUriRequest initRequest(String url) throws URISyntaxException,
			UnsupportedEncodingException {
		if (sessionId != null) {
			if (url.contains("?"))
				url += "&";
			else
				url += "?";

			List<BasicNameValuePair> params = new LinkedList<BasicNameValuePair>();
			params.add(new BasicNameValuePair(sessionKeyParam, sessionId));

			String paramString = URLEncodedUtils.format(params, URL_ENCODING);

			url += paramString;
		}
		HttpUriRequest request = null;
		URI uri;
		if (HttpGet.METHOD_NAME.equals(urlMethod)) {
			uri = new URI(url);
			request = new HttpGet(uri);
		} else if (HttpPost.METHOD_NAME.equals(urlMethod)) {
			uri = new URI(url);
			int i = url.indexOf('?');
			if (i == -1)
				request = new HttpPost(uri);
			else
				request = new HttpPost(new URI(url.substring(0, i)));

			List<NameValuePair> postParams = URLEncodedUtils.parse(uri,
					URL_ENCODING);
			if (postParams != null) {
				((HttpPost) request).setEntity(new UrlEncodedFormEntity(
						postParams));
				log.info("Post parameters : " + postParams);
			}
		} else {
			throw new UnsupportedOperationException(urlMethod + " unsupported");
		}
		String urlStr = uri.toString();
		int i = urlStr.indexOf('?');
		String referer;
		if (i == -1) {
			referer = urlStr;
		} else {
			referer = urlStr.substring(0, i);
		}
		request.setHeader("Host", uri.getHost());
		request.setHeader("User-Agent", userAgentString);
		request.setHeader("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		request.setHeader("Accept-Language", "en-US,en;q=0.5");
		request.setHeader("Cookie", getCookies(uri));
		request.setHeader("Connection", "keep-alive");
		request.setHeader("Referer", referer);
		request.setHeader("Content-Type", "application/x-www-form-urlencoded");

		return request;
	}

	public String getCookies(URI uri) {
		List<HttpCookie> cookies = cookieStore.get(uri);
		StringBuilder sb = new StringBuilder();
		for (HttpCookie cookie : cookies) {
			sb.append(cookie.getName()).append('=').append(cookie.getValue())
					.append("; ");
		}
		return sb.toString();
	}

	public void setCookies(HttpResponse response, URI uri) {
		for (Header h : response.getHeaders("Set-Cookie")) {
			HttpCookie cookie = new HttpCookie(h.getName(), h.getValue());
			cookieStore.add(uri, cookie);
			log.info("Setting Cookie:" + cookie);
		}
	}

	protected String executeRequest(URL url) {
		String responseStr = null;

		try {
			HttpUriRequest request = initRequest(url.toString());
			// If we get an HTTP 401 Unauthorized with
			// a challenge to solve.
			HttpResponse response = execute(request, context);
			// Validate that we got an HTTP 401 back
			if (respCode == HttpStatus.SC_UNAUTHORIZED) {
				if (response.containsHeader(WWW_AUTHENTICATE)) {
					shutdownClient();

					// Generate a solution Authentication header using your
					// username and password.
					final Header solution = getAuthHeader(response);
					if (solution != null) {
						log.info("auth header:" + solution.getName() + ":"
								+ solution.getValue());
						// Do another request, but this time include the
						// solution
						// Authentication header as generated by HttpClient.
						request.addHeader(solution);
						try {
							response = execute((HttpUriRequest) request,
									context);
						} catch (Exception e) {
							setErrStr("Exception connecting to server:", e);
						}
					}
				} else {
					setErrStr("Service responded with Http 401, "
							+ "but did not send us usable WWW-Authenticate header.");
				}

			}
			if (respCode == HttpStatus.SC_OK) {
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					if (contentType.contains("text")) {
						responseStr = EntityUtils.toString(entity);
						log.info("responseStr:" + responseStr);
					} else if (contentType.contains("image")) {
						try {
							savedImg = ImageIO.read(entity.getContent());
							len = entity.getContentLength();
							responseStr = "Read " + len + " bytes of image";
							if (saveImage) {
								String fname = savePath + "/" + getName() + "."
										+ fileDateTimeFmt.format(new Date())
										+ ".png";
								File outputfile = new File(fname);
								log.warn("Saving image to:"
										+ outputfile.getAbsolutePath());
								ImageIO.write(savedImg, "png", outputfile);
							}
						} catch (Exception e) {
							e.printStackTrace();
							len = entity.getContentLength();
							responseStr = "Read " + len + " bytes of image";
							if (saveImage) {

								String fname = savePath
										+ "/"
										+ getName()
										+ "."
										+ fileDateTimeFmt.format(new Date())
										+ "."
										+ contentType.substring(contentType
												.indexOf('/') + 1);
								File outputfile = new File(fname);
								try (FileOutputStream fos = new FileOutputStream(
										outputfile)) {
									responseStr = EntityUtils.toString(entity);
									fos.write(responseStr.getBytes());
								} catch (IOException e1) {
									e1.printStackTrace();
									log.error("exists:" + outputfile.exists());
									log.error("writable:"
											+ outputfile.canWrite());
									log.error("dir writable:"
											+ outputfile.getParentFile()
													.canWrite());
								}
							}
						}

					} else if (contentType.contains(OWL_CONTENT_TYPE)) {
						responseStr = EntityUtils.toString(entity);
						log.info("responseStr:" + responseStr);
					} else {
						responseStr = "content type:" + contentType;
					}

					EntityUtils.consume(entity);
				}
			} else if (respCode == HttpStatus.SC_FORBIDDEN) {
				StringBuilder sb = new StringBuilder();
				for (String key : conHeaders.keySet()) {
					sb.append(key).append(":").append(conHeaders.get(key))
							.append('\n');
				}
				responseStr = sb.toString();
			} else {
				setErrStr("URL returned:" + respCode);
			}
		} catch (Exception e) {
			setErrStr("Failed reading URL", e);
			respCode = HttpStatus.SC_GATEWAY_TIMEOUT;
		} finally {
			shutdownClient();
		}

		return responseStr;
	}

	protected String getBasicAuth() {
		String authString = login + ":" + password;
		log.debug("auth string: " + authString);
		byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
		String authStringEnc = new String(authEncBytes);
		log.info("Base64 encoded auth string: " + authStringEnc);
		Authenticator.setDefault(new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(login, password.toCharArray());
			}
		});

		return "Basic " + authStringEnc;
	}

	public String getUrl() {
		String result = null;
		long startTime = System.currentTimeMillis();
		try {
			HttpURLConnection con = connect();

			try {
				respCode = con.getResponseCode();
			} catch (Exception e) {
				tryTlsPlusCertInstall(con, e);
			}
			checkHeaders(con);
			// if connects OK do the read just to be sure
			if (respCode == HttpURLConnection.HTTP_OK || ignoreRespCode) {
				result = getUrlContentAsString(con);
			} else if (respCode == HttpURLConnection.HTTP_MOVED_TEMP
					|| ignoreRespCode) {
				result = getUrlContentAsString(con);
			} else if (respCode == HttpURLConnection.HTTP_FORBIDDEN
					|| ignoreRespCode) {
				StringBuilder sb = new StringBuilder();
				for (String key : conHeaders.keySet()) {
					sb.append(key).append(":").append(conHeaders.get(key))
							.append('\n');
				}
				result = sb.toString();

			} else {
				setErrStr("Failed:" + respCode + ": "
						+ con.getResponseMessage());
			}
		} catch (Exception e) {
			setErrStr(getName(), e);
		}
		timeToRun = System.currentTimeMillis() - startTime;
		return result;
	}

	public URL getHttpsURL() {
		return httpsURL;
	}

	public Date getModDate() {
		return modDate;
	}

	public long getLen() {
		return len;
	}

	public int getRespCode() {
		return respCode;
	}

	public String getKeyFile() {
		return keyFile;
	}

	public String getKeyPass() {
		return keyPass;
	}

	public int getRetries() {
		return retries;
	}

	public long getTimeToRun() {
		return timeToRun;
	}

	public String getCheckString() {
		return checkString;
	}

	public void setCheckString(String checkString) {
		this.checkString = checkString;
	}

	public boolean getFoundString() {
		return foundString;
	}

	protected boolean checkResponse(String s) {
		if (checkString != null) {
			foundString = s.indexOf(checkString) > -1;
			if (!foundString) {
				setErrStr("Failed to find:" + checkString + " in response");
			}
		}
		if (failString != null) {
			foundString = s.indexOf(failString) < 0;
			if (!foundString) {
				setErrStr("Found:" + failString + " in response");
			}
		}
		if (pattern != null) {
			Matcher matcher = pattern.matcher(s);
			foundString = matcher.find();
			if (!foundString) {
				setErrStr("Failed to find:" + pattern.toString()
						+ " in response");
			}
		}
		if (!foundString) {
			if (s != null) {
				setDetails(s);
			} else {
				StringBuilder sb = new StringBuilder();
				if (conHeaders != null) {
					sb.append("Connection Headers:<br>");
					for (String key : conHeaders.keySet()) {
						sb.append(key).append(":").append(conHeaders.get(key))
								.append("<br>");
					}
				}
				if (respHeaders != null) {
					sb.append("Response Headers:<br>");
					for (Header header : respHeaders) {
						sb.append(header.getName()).append(":")
								.append(header.getValue()).append("<br>");
					}
				}
				setDetails(sb.toString());
			}
		}

		return foundString;
	}

	public void run() {
		running = true;
		log.warn("reading url");
		for (int i = 0; i < retries; i++) {
			if (loginURL != null) {
				String rsp = login();
				if (rsp != null && rsp.contains("IDS_WEB_ID_PWD_ERROR")) {
					setDetails(rsp);
					setState("Login failed");

				} else {
					String s = executeRequest(httpsURL);
					log.info(s);
					if (respCode == HttpURLConnection.HTTP_OK
							|| respCode == HttpURLConnection.HTTP_FORBIDDEN) {
						if (checkResponse(s)) {
							setDetails(s);
							break;
						}
					}
				}

			} else {
				String s = getUrl();
				log.info(s);
				if (respCode == HttpURLConnection.HTTP_OK
						|| respCode == HttpURLConnection.HTTP_FORBIDDEN) {
					if (checkResponse(s)) {
						setDetails(s);
						break;
					}
				}

			}
		}
		// if the check failed and we have a reset URL run it before second
		// check. But run it only once per cycle
		if (reset != null && !foundString) {
			try {
				reset.doReset(getName());
			} catch (ClassNotFoundException | InstantiationException
					| IllegalAccessException e) {
				setDetails("Reset failed:" + e.getMessage());
			}
		}
		setState(foundString);
		running = false;
		log.warn("read url");
	}

	public Logger getLog() {
		return log;
	}

	protected URL getURL(String key) {
		return getURL(key, null);
	}

	protected URL getURL(String key, String parm) {
		URL rtn = null;
		String url = getBundleVal(String.class, key, null);
		if (url != null) {
			if (parm != null) {
				url += parm;
			}
			try {
				rtn = new URL(url);
			} catch (MalformedURLException e) {
				setErrStr(key + " is not a valid url:" + url, e);
			}
		}
		return rtn;
	}

	public void loadBundle() {
		httpsURL = getURL("httpsURL");
		loginURL = getURL("loginURL");
		loginNameParam = getBundleVal(String.class, "login.name", null);
		loginPasswordParam = getBundleVal(String.class, "login.password", null);
		loginParam = getBundleVal(String.class, "login.parms", null);
		sessionKeyParam = getBundleVal(String.class, "sessionKey", null);
		keyFile = getBundleVal(String.class, "keyFile", keyFile);
		keyPass = getBundleVal(String.class, "keyPass", keyPass);
		login = getBundleVal(String.class, "login", login);
		password = getBundleVal(String.class, "password", password);
		timeout = getBundleVal(Integer.class, "timeout", timeout);
		checkString = getBundleVal(String.class, "checkString", checkString);
		failString = getBundleVal(String.class, "failString", failString);
		urlMethod = getBundleVal(String.class, "urlMethod", HttpGet.METHOD_NAME);

		String regexString = getBundleVal(String.class, "regexString", null);
		if (regexString != null) {
			pattern = Pattern.compile(regexString);
		}
		followRedirects = getBundleVal(Boolean.class, "followRedirects",
				followRedirects);
		authType = getBundleVal(String.class, "authType", authType);
		saveImage = getBundleVal(Boolean.class, "saveImage", false);
		savePath = getBundleVal(String.class, "savePath", savePath);

		if (login == null) {
			log.info("Auth info not provided");
		}
	}

	public Map<String, List<String>> getConHeaders() {
		return conHeaders;
	}

	public Header[] getRespHeaders() {
		return respHeaders;
	}

	public boolean isSaveImage() {
		return saveImage;
	}

	public void setSaveImage(boolean saveImage) {
		this.saveImage = saveImage;
	}

	public String getSavePath() {
		return savePath;
	}

	public void setSavePath(String savePath) {
		this.savePath = savePath;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CheckUrl [");
		builder.append(super.toString());
		builder.append(",retries=");
		builder.append(retries);
		builder.append(", httpsURL=");
		builder.append(httpsURL);
		builder.append(", loginURL=");
		builder.append(loginURL);
		builder.append(", loginNameParam=");
		builder.append(loginNameParam);
		builder.append(", loginPasswordParam=");
		builder.append(loginPasswordParam);
		builder.append(", loginParam=");
		builder.append(loginParam);
		builder.append(", sessionKeyParam=");
		builder.append(sessionKeyParam);
		builder.append(", keyFile=");
		builder.append(keyFile);
		builder.append(", keyPass=");
		builder.append(keyPass);
		builder.append(", login=");
		builder.append(login);
		builder.append(", password=");
		builder.append(password);
		builder.append(", authType=");
		builder.append(authType);
		builder.append(", timeout=");
		builder.append(timeout);
		builder.append(", checkString=");
		builder.append(checkString);
		builder.append(", pattern=");
		builder.append(pattern);
		builder.append(", followRedirects=");
		builder.append(followRedirects);
		builder.append(", saveImage=");
		builder.append(saveImage);
		builder.append(", savePath=");
		builder.append(savePath);
		builder.append(", modDate=");
		builder.append(modDate);
		builder.append(", len=");
		builder.append(len);
		builder.append(", respCode=");
		builder.append(respCode);
		builder.append(", timeToRun=");
		builder.append(timeToRun);
		builder.append(", foundString=");
		builder.append(foundString);
		builder.append(", ignoreRespCode=");
		builder.append(ignoreRespCode);
		builder.append(", conHeaders=");
		builder.append(conHeaders);
		builder.append(", respHeaders=");
		builder.append(Arrays.toString(respHeaders));
		builder.append(", sessionId=");
		builder.append(sessionId);
		builder.append(", urlMethod=");
		builder.append(urlMethod);
		builder.append(", sslSocketFactory=");
		builder.append(sslSocketFactory);
		builder.append(", httpclient=");
		builder.append(httpclient);
		builder.append(", cookieStore=");
		builder.append(cookieStore);
		builder.append(", headerDateFormatFull=");
		builder.append(headerDateFormatFull);
		builder.append(", headerDateFormat=");
		builder.append(headerDateFormat);
		builder.append(", userAgentString=");
		builder.append(userAgentString);
		builder.append(", context=");
		builder.append(context);
		builder.append("]");
		return builder.toString();
	}

	private static class SavingTrustManager implements X509TrustManager {

		private final X509TrustManager tm;
		private X509Certificate[] chain;

		SavingTrustManager(X509TrustManager tm) {
			this.tm = tm;
		}

		public X509Certificate[] getAcceptedIssuers() {
			throw new UnsupportedOperationException();
		}

		public void checkClientTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
			throw new UnsupportedOperationException();
		}

		public void checkServerTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
			this.chain = chain;
			tm.checkServerTrusted(chain, authType);
		}
	}

	/**
	 * for testing
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		CheckBase item = new CheckUrl();
		item.cmd(args);
	}
}
