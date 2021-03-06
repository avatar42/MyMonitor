package dea.monitor.checker;

import java.io.InputStream;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import master.ExecuteSecureShellCommand.MyLogger;

/**
 * Check a URL to make sure it is readable. Needs keystore in working dir
 * keytool -genkey -alias dea42.com -keyalg RSA -keystore keystorefile.store
 * -keysize 2048
 * 
 * @author dea
 * 
 */
public class CheckSsh extends CheckBase {
	// Username of ssh account on remote machine
	private String user;
	// Hostname of the remote machine (eg:inst.eecs.berkeley.edu)
	private String host;
	// Password associated with your ssh account
	private String password;
	// Remote command
	private String command;
	protected int retries = 2;
	protected String checkString = null;
	protected int respCode = 0;
	protected boolean foundString = false;

	/**
	 * Set respCode and broadcastStatusCode To help keep codes separate
	 * broadcastStatusCode is BC_SSH_ERROR + (float) respCode / 100
	 * 
	 * @param respCode
	 */
	public void setRespCode(int respCode) {
		this.respCode = respCode;
		broadcastStatusCode = BC_SSH_ERROR + (float) respCode / 100;
	}

	@Override
	public void run() {
		running = true;
		log.warn("executing command");
		for (int i = 0; i < retries; i++) {
			StringBuilder sb = new StringBuilder();
			if (command != null) {
				JSch.setLogger(new MyLogger());
				JSch jsch = new JSch();

				Session session = null;
				Channel channel = null;
				try {
					session = jsch.getSession(user, host, 22);

					// TODO: might want to use client ssl certificate
					// here instead of a password
					session.setPassword(password);

					// Not recommended - skips host check
					session.setConfig("StrictHostKeyChecking", "no");

					// session.connect(); - ten second timeout
					session.connect(30 * 1000);

					channel = session.openChannel("exec");
					((ChannelExec) channel).setCommand(command);

					channel.setInputStream(null);

					((ChannelExec) channel).setErrStream(System.err);

					// Set the destination for the data sent back (from the
					// server)
					// TODO: You will probably want to send the response
					// somewhere other
					// than System.out
					channel.setOutputStream(System.out);

					// channel.connect(); - fifteen second timeout
					channel.connect(15 * 1000);

					InputStream in = channel.getInputStream();

					channel.connect();

					byte[] tmp = new byte[1024];
					while (true) {
						while (in.available() > 0) {
							int c = in.read(tmp, 0, 1024);
							if (c < 0)
								break;
							String s = new String(tmp, 0, c);
							log.debug("read:" + s);
							sb.append(s);
						}
						if (channel.isClosed()) {
							if (in.available() > 0)
								continue;
							log.debug("exit-status: " + channel.getExitStatus());
							setRespCode(channel.getExitStatus());
							break;
						}
						try {
							Thread.sleep(1000);
						} catch (Exception ee) {
						}
					}
				} catch (Exception e) {
					setErrStr(e.getMessage());
					broadcastStatusCode = BC_EXCEPTION;
				} finally {

					// Disconnect (close connection, clean up system resources)
					if (channel != null) {
						channel.disconnect();
					}
					if (session != null) {
						session.disconnect();
					}
				}
				String s = sb.toString();
				log.info(s);
				setDetailsAsHtml(s);
				if (respCode == 0) {
					if (checkString != null) {
						foundString = s.indexOf(checkString) > -1;
						if (!foundString) {
							setErrStr("Failed to find:" + checkString + " in response");
							broadcastStatusCode = BC_CONTENT_BAD;
						} else {
							broadcastStatusCode = BC_OK;
						}
					}
					break;
				} else {
					setErrStr("Command failed with exit code:" + respCode);
				}
			}
		}
		broadcastStatus();
		setState(foundString);
		running = false;
		log.warn("executed command");
	}

	@Override
	protected void loadBundle() {
		user = getBundleVal(String.class, "user", null);
		host = getBundleVal(String.class, "host", null);
		password = getBundleVal(String.class, "password", null);
		command = getBundleVal(String.class, "command", null);
		checkString = getBundleVal(String.class, "checkString", checkString);
		broadcastType = getBundleVal(String.class, "broadcastType", "ssh");
		broadcastAddr = getBundleVal(String.class, "broadcastAddr", host);

	}

	/**
	 * for testing
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		CheckBase item = new CheckSsh();
		item.cmd(args);
	}
}
