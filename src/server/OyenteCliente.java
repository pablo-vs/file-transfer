package server;

import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

import proto.*;

public class OyenteCliente implements Runnable {

	private final int BUFF_SIZE = 8192;
	private final Socket socket;
	private final Servidor servidor;
	private boolean keepGoing;
	private ObjectInputStream input;
	private ObjectOutputStream output;
	private InetAddress localhost, clientDir;
	private Usuario usuario;

	private Logger log = Logger.getLogger("OYENTE_CLIENTE");
	private Handler logH = new ConsoleHandler();


	OyenteCliente(Socket s, Servidor serv) {
		log.addHandler(logH);
		socket = s;
		servidor = serv;
		keepGoing = true;
	}

	public void run() {

		try (socket) {

			log.fine("Starting listener...");

			localhost = InetAddress.getLocalHost();

			output = new ObjectOutputStream(socket.getOutputStream());
			input = new ObjectInputStream(socket.getInputStream());

			TipoMensaje [] posiblesMensajes = TipoMensaje.values();

			log.fine("Listening for messages...");
			
			while(keepGoing) {

				Mensaje recibido = (Mensaje) input.readObject();

				log.fine("Message received.");

				switch(posiblesMensajes[recibido.getTipo()]) {
					case MENSAJE_CONEXION:
						{
							log.fine("MENSAJE_CONEXION.");
							MensajeConexion m = (MensajeConexion) recibido;
							handleConexion(m);
							break;
						}
					case MENSAJE_LISTA_USUARIOS:
						{
							log.fine("MENSAJE_LISTA_USUARIOS.");
							MensajeListaUsuarios m = (MensajeListaUsuarios) recibido;
							handleListaUsuarios(m);
							break;
						}
					case MENSAJE_CERRAR_CONEXION:
						{
							log.fine("MENSAJE_CERRAR_CONEXION.");
							MensajeCerrarConexion m = (MensajeCerrarConexion) recibido;
							handleCerrarConexion(m);
							break;
						}
				}
			}
			output.close();

		} catch (IOException | ClassNotFoundException e) {
			log.log(Level.SEVERE, "Error: ", e);
		}
	}

	private void handleConexion(MensajeConexion m) throws IOException {
		clientDir = InetAddress.getByName(m.getOrigen());
		usuario = m.usuario;
		boolean res = servidor.addUsuario(usuario);
		MensajeConfirmacionConexion resp = 
			new MensajeConfirmacionConexion(
					localhost.getHostAddress(),
					clientDir.getHostAddress());
		output.writeObject(resp);
		log.fine("Response sent.");
	}

	private void handleCerrarConexion(MensajeCerrarConexion m) throws IOException {
		boolean res = servidor.removeUsuario(usuario);
		MensajeConfirmacionCerrar resp =
			new MensajeConfirmacionCerrar(
					localhost.getHostAddress(),
					clientDir.getHostAddress());
		output.writeObject(resp);
		log.fine("Response sent.");
		keepGoing = false;
	}

	private void handleListaUsuarios(MensajeListaUsuarios m) throws IOException {
		Usuario[] lista = servidor.getListaUsuarios();
		MensajeConfirmacionLista resp =
			new MensajeConfirmacionLista(
					localhost.getHostAddress(),
					clientDir.getHostAddress(),
					lista);
		output.writeObject(resp);
		log.fine("Response sent.");
	}


}
