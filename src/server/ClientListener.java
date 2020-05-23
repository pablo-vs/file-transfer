package server;

import java.net.Socket;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.FileReader;

public class ClientListener implements Runnable {

	private final int BUFF_SIZE = 8192;
	private final Socket socket;
	private BufferedReader reader;
	private PrintWriter writer;

	ClientListener(Socket s) {
		socket = s;
	}

	public void run() {
		try (socket) {
			InputStream input = socket.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(input));

			OutputStream output = socket.getOutputStream();
			PrintWriter writer = new PrintWriter(output, true);

			String filename = reader.readLine();
			System.out.println("SERVER: " + filename + " Requested.");
			FileReader file = new FileReader(filename);
			
			System.out.println("SERVER: sending file...");
			int count;
			char[] buffer = new char[BUFF_SIZE];
			while((count = file.read(buffer)) > 0) {
				System.out.println("SERVER: writing...");
				writer.write(buffer, 0, count);
			}

			file.close();
			writer.close();
			System.out.println("SERVER: finished file transfer.");

		} catch (IOException e) {
			System.out.println("SERVER:");
			System.out.println(e);
		}
	}

}
