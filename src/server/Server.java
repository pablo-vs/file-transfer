package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.IOException;


public class Server {
	public static void main(String [] args) {
		
		if (args.length < 1) return;

		int port = Integer.parseInt(args[0]);

		try (ServerSocket serverSocket = new ServerSocket(port)) {

			System.out.println("SERVER: Initializing...");
			while(true) {
				Socket socket = serverSocket.accept();
				System.out.println("SERVER: Received incoming connection.");
				new Thread(new ClientListener(socket)).start();
			}

		} catch (IOException e) {
		}
	};

}
