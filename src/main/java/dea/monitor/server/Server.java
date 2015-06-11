package dea.monitor.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpServer;

import dea.monitor.checker.CheckHttpHandler;

@SuppressWarnings("restriction")
public class Server {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	public Server(int port, List<CheckHttpHandler> handlers) throws IOException {
		if (handlers != null && !handlers.isEmpty()) {
			InetSocketAddress addr = new InetSocketAddress("192.168.1.183",
					port);

			HttpServer server = HttpServer.create(addr, 0);
			for (CheckHttpHandler handler : handlers) {
				server.createContext("/", handler);// + handler.getName()
			}
			server.setExecutor(null);// Executors.newCachedThreadPool());
			server.start();
			log.info("Server is listening on port " + port);
		}
	}

	/**
	 * for unit testing
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			List<CheckHttpHandler> handlers = new ArrayList<CheckHttpHandler>();
			for (String arg : args) {
				CheckHttpHandler handler = new CheckHttpHandler();
				handler.loadBundle(arg);
				handlers.add(handler);
			}
			new Server(80, handlers);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
