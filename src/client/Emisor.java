package client;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.Handler;
import java.util.logging.ConsoleHandler;

import proto.Usuario;

public class Emisor extends Thread implements Conexion {
	
	private final int BUFF_SIZE = 8192;
	public final Path file;
	public final Usuario destino;
	public final int port;

	private Cliente cliente;

	private static Logger log = Logger.getLogger("EMISOR");
	private static Handler logH;

	public Emisor(Path f, Usuario u, int p, Cliente c) {
		if (logH == null) {
			logH = new ConsoleHandler();
			log.addHandler(logH);
		}
		file = f;
		destino = u;
		port = p;
		cliente = c;
	}

	public void run() {
		log.fine("Starting SocketServer...");
		try (ServerSocket server = new ServerSocket(port)) {
			log.fine("Server started: awaiting...");
			Socket socket = server.accept();
			OutputStream output = socket.getOutputStream();

			InputStream input = Files.newInputStream(file);

			log.fine("Connection recived. Transmitting file...");

			int count;
			byte[] buffer = new byte[BUFF_SIZE];
			while((count = input.read(buffer)) > 0) {
				// Latency test
				Thread.sleep(60000);
				output.write(buffer, 0, count);
			}

			input.close();
			output.close();
			log.fine("File transmited");
			cliente.onFinConexion(this);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Error: ", e);
		} catch(Exception e) {}
	}

	@Override
	public int getTipo() {
		return 1; // Saliente
	}

	@Override
	public int getPuerto() {
		return port;
	}

	@Override
	public String getPeer() {
		return destino.iden;
	}

	@Override
	public String getFilename() {
		return file.getFileName().toString();
	}
}



