package client;

import java.net.Socket;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.net.InetAddress;

import proto.*;

public class Cliente {
	
	private InetAddress serverDir, localhost;
	private int port;
	private String iden;
	private ArrayList<String> ficheros;

	public static void main(String [] args) {
		
		if (args.length < 3) return;

		InetAddress servDir = InetAddress.getByName(args[0]);
		int p = Integer.parseInt(args[1]);
		String id = args[2];
		ArrayList<String> fich = new ArrayList<String>(Arrays.asList("fich1", "fich2"));

		Cliente c = new Cliente();
		c.init(servDir, p, id, fich);

	};

	void init(InetAddress host, int p, String id, List<String> fich) {
		serverDir = host;
		port = p;
		iden = id;
		ficheros = new ArrayList<String>(fich);
		InetAddress localhost = InetAddress.getLocalHost();

		
		System.out.println("CLIENT: Attempting to connect...");
		try (Socket socket = new Socket(serverDir, port)) {

			ObjectInputStream input =
				new ObjectInputStream(socket.getInputStream());

			ObjectOutputStream output =
				new ObjectOutputStream(socket.getOutputStream());

			Usuario usu = new Usuario(iden, localhost, ficheros);
			MensajeConexion con = new MensajeConexion(
					serverDir.getHostAddress(), localhost.getHostAddress(), usu);

			output.writeObject(con);

			Mensaje mens = (Mensaje) input.readObject();
			System.out.println("CLIENT: Connection successful");


			//writer.println(filename);


			//String response;
			//while(!reader.ready());
			//while((response = reader.readLine()) != null)
				//System.out.println(response);

		} catch (IOException e) {
			System.out.println("CLIENT:");
			System.out.println(e);
		}
	}

}
