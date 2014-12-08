package dea.monitor.tools;

public class ScanResult {
	private int port;
	private boolean isOpen;
	private boolean udp;

	public ScanResult(int port, boolean isOpen) {
		this(port, isOpen, false);
	}

	public ScanResult(int port, boolean isOpen, boolean udp) {
		super();
		this.port = port;
		this.isOpen = isOpen;
		this.udp = udp;
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

}
