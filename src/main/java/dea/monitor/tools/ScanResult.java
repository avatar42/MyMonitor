package dea.monitor.tools;

public class ScanResult {
	private int port;
	private boolean isOpen;
	private boolean udp;
	private String read;

	public ScanResult(int port, boolean isOpen, String read) {
		this(port, isOpen, false, read);
	}

	public ScanResult(int port, boolean isOpen, boolean udp, String read) {
		super();
		this.port = port;
		this.isOpen = isOpen;
		this.udp = udp;
		this.read = read;
	}

	public int getPort() {
		return port;
	}

	public boolean isOpen() {
		return isOpen;
	}

	public boolean isUdp() {
		return udp;
	}

	public String getRead() {
		return read;
	}

}
