package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.IOException;

class Server {
	public static void main(String [] args) {
		
		if (args.length < 1) return;

		int port = Integer.parseInt(args[0]);

		try (ServerSocket serverSocket = new ServerSocket(port)) {

			while(true) {
				Socket socket = serverSocket.accept();

				InputStream input = socket.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(input));
				String userId = reader.readLine();

				OutputStream output = socket.getOutputStream();
				PrintWriter writer = new PrintWriter(output, true);

				writer.println("Simple File Protocol: User ID=" + userId);

				socket.close();
			}

		} catch (IOException e) {
			
		}
	};

}
