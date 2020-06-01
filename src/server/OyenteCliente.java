package server;

import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

import java.util.List;

import java.util.concurrent.locks.ReentrantLock;

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

	private ReentrantLock lockOutput = new ReentrantLock(true);

	private static Logger log = Logger.getLogger(OyenteCliente.class.getName());
	private static Handler logH;


	OyenteCliente(Socket s, Servidor serv) {
		if (logH == null) {
			logH = new ConsoleHandler();
			log.addHandler(logH);
		}
		socket = s;
		servidor = serv;
		keepGoing = true;
	}

	public void run() {

		try (socket) {

			log.fine("Iniciando oyente...");

			localhost = InetAddress.getLocalHost();

			output = new ObjectOutputStream(socket.getOutputStream());
			input = new ObjectInputStream(socket.getInputStream());

			TipoMensaje [] posiblesMensajes = TipoMensaje.values();

			log.fine("Esperando mensajes...");
			
			while(keepGoing) {

				Mensaje recibido = (Mensaje) input.readObject();

				log.fine("Mensaje recibido.");
				log.fine(posiblesMensajes[recibido.getTipo()].name());

				switch(posiblesMensajes[recibido.getTipo()]) {
					case MENSAJE_CONEXION:
						{
							MensajeConexion m = (MensajeConexion) recibido;
							handleConexion(m);
							break;
						}
					case MENSAJE_LISTA_USUARIOS:
						{
							MensajeListaUsuarios m = (MensajeListaUsuarios) recibido;
							handleListaUsuarios(m);
							break;
						}
					case MENSAJE_PEDIR_FICHERO:
						{
							MensajePedirFichero m = (MensajePedirFichero) recibido;
							handlePedirFichero(m);
							break;
						}
					case MENSAJE_PREPARADO_CLIENTESERVIDOR:
						{
							MensajePreparadoCS m = (MensajePreparadoCS) recibido;
							handlePreparadoCS(m);
							break;
						}
					case MENSAJE_ACTUALIZAR:
						{
							MensajeActualizar m = (MensajeActualizar) recibido;
							handleActualizar(m);
							break;
						}
					case MENSAJE_CERRAR_CONEXION:
						{
							MensajeCerrarConexion m = (MensajeCerrarConexion) recibido;
							handleCerrarConexion(m);
							break;
						}
				}
			}
			log.fine("Cerrada la conexion con " + usuario.iden);
			output.close();

		} catch (IOException | ClassNotFoundException e) {
			log.log(Level.SEVERE, "Error: ", e);
		}
	}

	private void handleConexion(MensajeConexion m) throws IOException {
		clientDir = InetAddress.getByName(m.getOrigen());
		usuario = m.usuario;
		boolean res = servidor.addUsuario(usuario, this);
		if (!res)
			log.severe("Error: No se ha podido añadir el usuario " + usuario.iden);
		MensajeConfirmacionConexion resp = 
			new MensajeConfirmacionConexion(
					localhost.getHostAddress(),
					clientDir.getHostAddress());
		writeMensaje(resp);
		log.fine("Respuesta enviada.");
	}

	private void handleCerrarConexion(MensajeCerrarConexion m) throws IOException {
		boolean res = servidor.removeUsuario(usuario);
		if(!res)
			log.severe("Error: No se ha podido eliminar el usuario " + usuario.iden);
		MensajeConfirmacionCerrar resp =
			new MensajeConfirmacionCerrar(
					localhost.getHostAddress(),
					clientDir.getHostAddress());
		writeMensaje(resp);
		log.fine("Respuesta enviada.");
		keepGoing = false;
	}

	private void handleListaUsuarios(MensajeListaUsuarios m) throws IOException {
		Usuario[] lista = servidor.getListaUsuarios();
		MensajeConfirmacionLista resp =
			new MensajeConfirmacionLista(
					localhost.getHostAddress(),
					clientDir.getHostAddress(),
					lista);
		writeMensaje(resp);
		log.fine("Respuesta enviada.");
	}

	private void handlePedirFichero(MensajePedirFichero m) throws IOException {
		List<String> candidatos = servidor.consultarFichero(m.fichero);
		if(candidatos == null)
			log.severe("Error: Fichero " + m.fichero + " no encontrado.");
		else {
			int elegido = (int)(Math.random() * (candidatos.size()));
			String iden = candidatos.get(elegido);
			OyenteCliente OC = servidor.getOyente(iden);
			OC.emitirFichero(m.fichero, usuario);
		}
	}

	private void handlePreparadoCS(MensajePreparadoCS m) throws IOException {
		log.fine("Cliente preparado");
		OyenteCliente OC = servidor.getOyente(m.usuario.iden);
		OC.preparadoSC(usuario, m.puerto, m.fichero);
	}

	// Llamado desde otro oyente, pide al usuario asociado
	// a este oyente que envíe el fichero dado al usuario dado
	private void emitirFichero(String fich, Usuario usu) throws IOException {
		MensajeEmitirFichero req = 
			new MensajeEmitirFichero(
					localhost.getHostAddress(),
					clientDir.getHostAddress(),
					fich, usu);
		log.fine("Solicitando la emisión de " + fich + " a " + usu.iden);
		writeMensaje(req);
	}

	// Llamado desde otro oyente que ha recibido un PreparadoCS
	private void preparadoSC(Usuario usu, int puerto, String fich) throws IOException {
		MensajePreparadoSC req =
			new MensajePreparadoSC(
					localhost.getHostAddress(),
					clientDir.getHostAddress(),
					usu, puerto, fich);
		log.fine("Enviando mensaje cliente preparado");
		writeMensaje(req);
	}

	private void handleActualizar(MensajeActualizar m) throws IOException {
		log.finer(String.valueOf(m.usuario.ficheros.size()));
		m.usuario.ficheros.forEach(s -> log.finer(s));
		servidor.actualizarUsuario(m.usuario);
		usuario = m.usuario;
	}

	// Hace un lock del flujo de salida y escribe un mensaje
	// Esto es necesario porque otros oyentes pueden enviar
	// mensajes por este flujo.
	private void writeMensaje(Mensaje m) throws IOException {
		lockOutput.lock();
		output.writeObject(m);
		lockOutput.unlock();
	}
	
}
