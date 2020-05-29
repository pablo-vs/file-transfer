package client;

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

public class Receptor extends Thread implements Conexion {
	
	private final int BUFF_SIZE = 8192;
	public final Path file;
	public final Usuario fuente;
	public final int port;

	private Cliente cliente;

	private static Logger log = Logger.getLogger("RECEPTOR");
	private static Handler logH;

	public Receptor(Path f, Usuario u, int p, Cliente c) {
		if (logH == null) {
			logH = new ConsoleHandler();
			log.addHandler(logH);
		}
		file = f;
		fuente = u;
		port = p;
		cliente = c;
	}

	public void run() {
		log.fine("Attempting to connect...");
		try (Socket socket = new Socket(fuente.dir, port)) {
			InputStream input = socket.getInputStream();

			OutputStream output = Files.newOutputStream(file);

			log.fine("Connected: receiving file...");
			int count;
			byte[] buffer = new byte[BUFF_SIZE];
			while((count = input.read(buffer)) > 0) {
				output.write(buffer, 0, count);
			}

			input.close();
			output.close();
			log.fine("Connected: File received.");
			cliente.onFinConexion(this);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Error: ", e);
		}
	}

	@Override
	public int getTipo() {
		return -1; // Entrante
	}

	@Override
	public int getPuerto() {
		return port;
	}

	@Override
	public String getPeer() {
		return fuente.iden;
	}

	@Override
	public String getFilename() {
		return file.getFileName().toString();
	}
}



