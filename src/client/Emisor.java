package client;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

public class Emisor extends Thread {
	
	private File file;
	private int port;

	private Client client;

	public Emisor() {

	}

	public void run() {
		ServerSocket server = new ServerSocket(port);
		try (Socket socket = server.accept()) {
			
		} catch (IOException e) {

		}
	}
}
