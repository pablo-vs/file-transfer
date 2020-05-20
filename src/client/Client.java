package client;

import java.net.Socket;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.IOException;

class Client {
	public static void main(String [] args) {
		
		if (args.length < 3) return;

		String hostname = args[0];
		int port = Integer.parseInt(args[1]);
		String userId = args[2];

		try (Socket socket = new Socket(hostname, port)) {

			InputStream input = socket.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(input));

			OutputStream output = socket.getOutputStream();
			PrintWriter writer = new PrintWriter(output, true);

			writer.println(userId);
			String response = reader.readLine();
			System.out.println(response);

		} catch (IOException e) {
			
		}
	};

}
