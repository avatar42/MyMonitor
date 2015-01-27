package dea.monitor.tools;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scans a host to find open TCP ports. If the port is closed to TCP then it
 * checks for UDP on that port as well.
 * 
 * @author dea
 * 
 */
public class PortScanner {
	protected static final Logger log = LoggerFactory
			.getLogger(PortScanner.class);

	public static Future<ScanResult> portIsOpen(final ExecutorService es,
			final String ip, final int port, final int timeout,
			final boolean runUdpcheck) {

		return es.submit(new Callable<ScanResult>() {
			@Override
			public ScanResult call() {
				String read = null;
				boolean foundOpenPort = false;
				boolean isUDP = false;
				try {
					log.info("Trying TCP:" + port);
					Socket socket = new Socket();
					socket.connect(new InetSocketAddress(ip, port), timeout);
					foundOpenPort = true;
					if (!socket.isOutputShutdown()) {
						log.info("Writing to TCP:" + port);
						DataOutputStream out = new DataOutputStream(socket
								.getOutputStream());
						out.writeBytes("\r\n");
						out.flush();
						if (!socket.isInputShutdown()) {
							log.info("Reading to TCP:" + port);
							BufferedReader in = new BufferedReader(
									new InputStreamReader(socket
											.getInputStream()));
							if (in.ready()) {
								read = in.readLine();
							}
						}
						out.close();
					}
					socket.close();
					if (foundOpenPort && (read == null || read.length() == 0)) {
						String tivoUrl = "http://" + ip;
						log.info("Trying:" + tivoUrl);
						URL url = new URL(tivoUrl);
						HttpURLConnection con = (HttpURLConnection) url
								.openConnection();
						con.setRequestMethod("GET");
						StringBuilder sb = new StringBuilder();
						Map<String, List<String>> headers = con
								.getHeaderFields();
						for (String key : headers.keySet()) {
							sb.append(key).append(":").append(headers.get(key))
									.append('\n');
						}
						try (BufferedReader reader = new BufferedReader(
								new InputStreamReader(con.getInputStream()))) {
							String line = null;
							read = "";
							while ((line = reader.readLine()) != null) {
								sb.append(line).append('\n');
							}
						} catch (Exception e) {
							// ignore
						}
						read = sb.toString();
					}
				} catch (Exception ex) {
					if (runUdpcheck) {
						log.info("TCP failed trying UDP:" + port);
						try {
							DatagramSocket clientSocket = new DatagramSocket();
							clientSocket.setSoTimeout(timeout);
							InetAddress IPAddress = InetAddress.getByName(ip);
							byte[] sendData = new byte[1024];
							byte[] receiveData = new byte[1024];
							String sentence = "test";
							sendData = sentence.getBytes();
							DatagramPacket sendPacket = new DatagramPacket(
									sendData, sendData.length, IPAddress, port);
							clientSocket.send(sendPacket);
							DatagramPacket receivePacket = new DatagramPacket(
									receiveData, receiveData.length);
							clientSocket.receive(receivePacket);
							read = new String(receivePacket.getData());
							log.info("FROM SERVER:" + read);
							clientSocket.close();
							foundOpenPort = true;
							isUDP = true;
						} catch (SocketTimeoutException e) {
						} catch (IOException e) {
							log.error("UDP scan of " + port + " caught ", e);
						}
					} else {
						if (!(ex instanceof ConnectException)
								&& !(ex instanceof SocketTimeoutException)) {
							log.error("TCP scan of " + port + " caught ", ex);
						}
					}
				}
				return new ScanResult(port, foundOpenPort, isUDP, read);
			}
		});
	}

	public static final void usage(String errMsg) {
		log.error(errMsg);
		System.err
				.println("USAGE:PortScanner [-u] ipToScan [timeout] [lowestPort] [highestPort] [maxThreads]");
		log.error("-u = skip UDP check");
		log.error("timeout in milliseconds defaults to 1000");
		log.error("lowestPort defaults to 1");
		log.error("highestPort defaults to 65535");
		log.error("maxThreads defaults to 2000");
		System.exit(-1);
	}

	/**
	 * Get an int from the from the command line args
	 * 
	 * @param argName
	 *            to use in error message if arg is not int
	 * @param defaultVal
	 *            what to return if arg not given
	 * @param arg
	 *            which arg to get
	 * @param args
	 *            passed
	 * @return args[arg] as int
	 */
	public static int getInt(String argName, int defaultVal, int arg,
			String... args) {
		if (args.length > arg) {
			try {
				return Integer.parseInt(args[arg]);
			} catch (NumberFormatException e) {
				StringBuilder sb = new StringBuilder();
				sb.append("PortScanner ");
				for (String a : args) {
					sb.append(a + " ");
				}
				log.error(sb.toString());
				usage(argName + ":" + args[arg] + " is not a number.");
			}
		}
		log.info("Using default value of " + defaultVal + " for " + argName);
		return defaultVal;
	}

	public static void main(final String... args) {
		if (args.length > 0) {
			Map<Integer, String> openTcp = new TreeMap<Integer, String>();
			Map<Integer, String> openUdp = new TreeMap<Integer, String>();
			boolean runUdpcheck = true;
			int arg = 0;
			while (args.length > arg && args[arg].startsWith("-")) {
				if (args[arg].contains("u")) {
					runUdpcheck = false;
				}
				arg++;
			}
			final String ip = args[arg++];
			int timeout = getInt("timeout", 1000, arg++, args);
			int firstPort = getInt("lowestPort", 1, arg++, args);
			int lastPort = getInt("highestPort", 65535, arg++, args);
			int maxThreads = getInt("maxThreads", 2000, arg++, args);
			if (maxThreads > lastPort - firstPort + 1) {
				maxThreads = lastPort - firstPort + 1;
			}
			final ExecutorService es = Executors.newFixedThreadPool(maxThreads);
			final List<Future<ScanResult>> futures = new ArrayList<>();
			long start = System.currentTimeMillis();
			log.info("Scanning " + firstPort + " thru " + lastPort + " of "
					+ ip);
			for (int port = firstPort; port <= lastPort; port++) {
				futures.add(portIsOpen(es, ip, port, timeout, runUdpcheck));
			}
			es.shutdown();
			int openPorts = 0;
			for (final Future<ScanResult> f : futures) {
				try {
					if (f.get().isOpen()) {
						openPorts++;
						if (f.get().isUdp()) {
							log.info("" + f.get().getPort() + " is open as UDP");
							openUdp.put(f.get().getPort(), f.get().getRead());
						} else {
							log.info("" + f.get().getPort() + " is open as TCP");
							openTcp.put(f.get().getPort(), f.get().getRead());
						}
					} else {
						log.debug("" + f.get().getPort() + " is closed.");
					}
				} catch (InterruptedException | ExecutionException e) {
					log.error("Scan failed", e);
				}
			}
			log.info("time:" + (System.currentTimeMillis() - start)
					+ " milsecs");
			log.info("There are " + openPorts + " open ports on host " + ip
					+ " in the range of " + firstPort + " to " + lastPort
					+ " (probed with a timeout of " + timeout + "ms)");
			log.info("TCP open ports are:" + openTcp);
			log.info("UDP open ports are:" + openUdp);
		} else {
			usage("IP of host not given");
		}
	}
}
