package dea.monitor.tools;

/**
 * http://blogs.sun.com/andreas/resource/InstallCert.java
 * Use:
 * java InstallCert hostname
 * Example:
 *% java InstallCert ecc.fedora.redhat.com
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class used to add the server's certificate to the KeyStore with your trusted
 * certificates.
 * 
 * @See 
 *      http://www.mkyong.com/webservices/jax-ws/suncertpathbuilderexception-unable
 *      -to-find-valid-certification-path-to-requested-target/ Modified to be
 *      callable from code and to take keystore arg
 */
public class InstallCert {
	protected static final Logger log = LoggerFactory
			.getLogger(InstallCert.class);

	public static final String DEFAULT_STORE = "jssecacerts";

	public static SSLSocketFactory install(String host, int port,
			String password, String keystore) throws KeyStoreException,
			NoSuchAlgorithmException, CertificateException, IOException,
			KeyManagementException {
		char[] passphrase = password.toCharArray();

		File file;
		if (keystore == null) {
			keystore = DEFAULT_STORE;
		}
		file = new File(keystore);
		if (file.isFile() == false) {
			char SEP = File.separatorChar;
			File dir = new File(System.getProperty("java.home") + SEP + "lib"
					+ SEP + "security");
			file = new File(dir, keystore);
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
			log.error(password, e1);
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

		log.info("Opening connection to " + host + ":" + port + "...");
		SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
		socket.setSoTimeout(10000);
		try {
			log.info("Starting SSL handshake...");
			socket.startHandshake();
			socket.close();
			log.info("");
			log.info("No errors, certificate is already trusted");
		} catch (SSLException e) {
			log.error("", e);

			X509Certificate[] chain = tm.chain;
			if (chain == null) {
				log.info("Could not obtain server certificate chain");
				return null;
			}

			log.info("");
			log.info("Server sent " + chain.length + " certificate(s):");
			log.info("");
			MessageDigest sha1 = MessageDigest.getInstance("SHA1");
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			for (int i = 0; i < chain.length; i++) {
				X509Certificate cert = chain[i];
				log.info(" " + (i + 1) + " Subject " + cert.getSubjectDN());
				log.info("   Issuer  " + cert.getIssuerDN());
				sha1.update(cert.getEncoded());
				log.info("   sha1    " + toHexString(sha1.digest()));
				md5.update(cert.getEncoded());
				log.info("   md5     " + toHexString(md5.digest()));
				log.info("");
			}
			int k = 0;
			if (keystore.equals(DEFAULT_STORE)) {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(System.in));

				System.out
						.println("Enter certificate to add to trusted keystore or 'q' to quit: [1]");
				String line = reader.readLine().trim();
				try {
					k = (line.length() == 0) ? 0 : Integer.parseInt(line) - 1;
				} catch (NumberFormatException ex) {
					log.error("KeyStore not changed", ex);
					return factory;
				}
			}
			X509Certificate cert = chain[k];
			String alias = host + "-" + (k + 1);
			ks.setCertificateEntry(alias, cert);

			OutputStream out = new FileOutputStream(keystore);
			ks.store(out, passphrase);
			out.close();

			log.info("");
			log.info(cert.toString());
			log.info("");
			log.info("Added certificate to keystore '" + keystore
					+ "' using alias '" + alias + "'");

		}
		return factory;
	}

	public static void main(String[] args) throws Exception {
		if ((args.length > 0) && (args.length < 4)) {
			String[] c = args[0].split(":");
			String host = c[0];
			int port = (c.length == 1) ? 443 : Integer.parseInt(c[1]);
			String p = (args.length == 1) ? "changeit" : args[1];
			String store = (args.length < 3) ? null : args[2];
			install(host, port, p, store);
		} else {
			System.out
					.println("Usage: java InstallCert <host>[:port] [passphrase] [keystore]");
			return;
		}
	}

	private static final char[] HEXDIGITS = "0123456789abcdef".toCharArray();

	private static String toHexString(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 3);
		for (int b : bytes) {
			b &= 0xff;
			sb.append(HEXDIGITS[b >> 4]);
			sb.append(HEXDIGITS[b & 15]);
			sb.append(' ');
		}
		return sb.toString();
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

}