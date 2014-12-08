package dea.monitor.tools;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PortScanner {
	public static Future<ScanResult> portIsOpen(final ExecutorService es,
			final String ip, final int port, final int timeout) {
		return es.submit(new Callable<ScanResult>() {
			@Override
			public ScanResult call() {
				try {
					Socket socket = new Socket();
					socket.connect(new InetSocketAddress(ip, port), timeout);
					socket.close();
					return new ScanResult(port, true);
				} catch (Exception ex) {
					try {
						DatagramSocket clientSocket = new DatagramSocket();
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
						String modifiedSentence = new String(receivePacket
								.getData());
						System.out.println("FROM SERVER:" + modifiedSentence);
						clientSocket.close();

						return new ScanResult(port, true, true);
					} catch (SocketException e) {
						return new ScanResult(port, false);
					} catch (IOException e) {
						System.err.println(e.getMessage());
						return new ScanResult(port, false);
					}
				}
			}
		});
	}

	public static void main(final String... args) {
		final ExecutorService es = Executors.newFixedThreadPool(2000);
		final String ip = "192.168.1.152";
		final int timeout = 1000;
		final List<Future<ScanResult>> futures = new ArrayList<>();
		long start = System.currentTimeMillis();
		for (int port = 1; port <= 65535; port++) {
			futures.add(portIsOpen(es, ip, port, timeout));
		}
		es.shutdown();
		int openPorts = 0;
		for (final Future<ScanResult> f : futures) {
			try {
				if (f.get().isOpen()) {
					openPorts++;
					if (f.get().isUdp())
						System.out.println("" + f.get().getPort()
								+ " is open as UDP");
					else
						System.out.println("" + f.get().getPort()
								+ " is open as TCP");
				}
			} catch (InterruptedException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println("time:" + (System.currentTimeMillis() - start)
				+ " milsecs");
		System.out.println("There are " + openPorts + " open ports on host "
				+ ip + " (probed with a timeout of " + timeout + "ms)");
	}

}
