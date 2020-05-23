package client;

import java.net.Socket;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.IOException;

public class Client {
	public static void main(String [] args) {
		
		if (args.length < 3) return;

		String hostname = args[0];
		int port = Integer.parseInt(args[1]);
		String filename = args[2];

		
		System.out.println("CLIENT: Attempting to connect...");
		try (Socket socket = new Socket(hostname, port)) {


			System.out.println("CLIENT: Connection successful");

			InputStream input = socket.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(input));

			OutputStream output = socket.getOutputStream();
			PrintWriter writer = new PrintWriter(output, true);

			writer.println(filename);


			String response;
			while(!reader.ready());
			while((response = reader.readLine()) != null)
				System.out.println(response);

		} catch (IOException e) {
			System.out.println("CLIENT:");
			System.out.println(e);
		}
	};

}
