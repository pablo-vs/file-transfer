package client;

import java.net.Socket;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.Handler;
import java.util.logging.ConsoleHandler;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import proto.*;
import util.Concurrent;

public class Cliente {

	private InetAddress serverDir, localhost;
	private int port;
	private String iden, fileDir;
	private OyenteServidor OS;
	private ObjectOutputStream output;
	private ObjectInputStream input;
	private ControladorCliente control;

	private static Logger log = Logger.getLogger("CLIENTE");
	private static Handler logH;

	private Concurrent<ArrayList<String>> ficheros;

	private Concurrent<HashMap<Integer, Conexion>> conexiones =
		new Concurrent<HashMap<Integer, Conexion>>();

	private Concurrent<HashMap<String, Usuario>> usuarios =
		new Concurrent<HashMap<String, Usuario>>();

	private Concurrent<HashMap<String, ArrayList<String>>> ficherosRemotos =
		new Concurrent<HashMap<String, ArrayList<String>>>();

	private Concurrent<Status> status = new Concurrent<Status>();

	public class Status {
		public boolean conectado = false, 
			esperandoRespuesta = false;
		public int emitiendo = 0,
			recibiendo = 0;

		public Status() {}
		public Status(Status st) {
			conectado = st.conectado;
			esperandoRespuesta = st.esperandoRespuesta;
			emitiendo = st.emitiendo;
			recibiendo = st.recibiendo;
		}
	}


	public Cliente(InetAddress dir, int p, String id, String fd,
		   	ControladorCliente cont) throws IOException {
		if (logH == null) {
			logH = new ConsoleHandler();
			log.addHandler(logH);
		}
		serverDir = dir;
		port = p;
		iden = id;
		fileDir = fd;
		control = cont;
		try {
			localhost = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			log.log(Level.SEVERE, "No se ha podido determinar la dirección local", e);
		}
		
		ficheros.lockAndSet(new ArrayList<String>(
			Files.list(Paths.get(fd))
			.map((x) -> x.getFileName().toString())
			.collect(Collectors.toList())));

	}

	void connect() {
		if (status.conectado) {
			control.printOutput("No se puede conectar: ya conectado.");
			control.setError();
			return;
		}
		if (status.esperandoRespuesta) {
			control.setError();
			control.printOutput("No se puede conectar: esperando respuesta");
			return;
		}
		log.fine("Intentando conectar...");
		Socket socket;
		try {
			socket = new Socket(serverDir, port);

			output = new ObjectOutputStream(socket.getOutputStream());

			input = new ObjectInputStream(socket.getInputStream());

			OS = new OyenteServidor(input, output, this);
			OS.start();

			Usuario usu = new Usuario(iden, localhost, ficheros);
			MensajeConexion con = new MensajeConexion(
					serverDir.getHostAddress(), localhost.getHostAddress(), usu);

			output.writeObject(con);
			log.fine("Conexión solicitada.");

			statusLock.lock();
			status.esperandoRespuesta = true;
			statusLock.unlock();
			
		} catch (IOException e) {
			System.err.println("CLIENT:");
			System.err.println(e);
		}
	}

	void listaUsuarios() {
		if (!status.conectado) {
			control.printOutput("No se puede cargar lista: no conectado.");
			control.setError();
			return;
		}
		try {
			MensajeListaUsuarios req = new MensajeListaUsuarios
				(serverDir.getHostAddress(), localhost.getHostAddress());

			log.fine("Solicitando lista de usuarios...");
			output.writeObject(req);
			statusLock.lock();
			status.esperandoRespuesta = true;
			statusLock.unlock();
		} catch (IOException e) {
			System.err.println("CLIENT:");
			System.err.println(e);
		}
	}

	public void actualizarFicheros() {
		control.printOutput("Leyendo lista de ficheros locales...");
		try {
			ficheros = new ArrayList<String>(
				Files.list(Paths.get(fileDir))
				.map((x) -> x.getFileName().toString())
				.collect(Collectors.toList()));
			control.printOutput("Lista actualizada");
			enviarActualizar();
		} catch (IOException e) {
			control.printOutput("No se ha podido leer la lista.");
			log.log(Level.SEVERE, "Error: ", e);
		}
	}

	public void enviarActualizar() {
		if (!status.conectado) {
			log.fine("No se envía mensaje de actualización: no conectado");
			return;
		}
		try {
			Usuario usu = new Usuario(iden, localhost, ficheros);
			MensajeActualizar req = new MensajeActualizar
				(serverDir.getHostAddress(), localhost.getHostAddress(),
				 usu);
			output.writeObject(req);
			log.fine("Actualización enviada al servidor");
		} catch (IOException e) {
			System.err.println("CLIENT:");
			System.err.println(e);
		}
	}

	void pedirFichero(String fich) {
		if (!status.conectado) {
			control.printOutput("No se puede pedir fichero: no conectado.");
			control.setError();
			return;
		}
		if (!ficherosRemotos.containsKey(fich)) {
			control.printOutput("Fichero desconocido");
			control.setError();
			return;
		}
		try {
			MensajePedirFichero req = new MensajePedirFichero
				(serverDir.getHostAddress(), localhost.getHostAddress(),
				 fich);

			control.printOutput("Pidiendo fichero " + fich);
			output.writeObject(req);
			statusLock.lock();
			status.esperandoRespuesta = true;
			statusLock.unlock();
		} catch (IOException e) {
			System.err.println("CLIENT:");
			System.err.println(e);
		}
	}

	void disconnect() {
		if (!status.conectado) {
			control.printOutput("No se puede desconectar: no conectado.");
			control.setError();
			return;
		}
		if (status.esperandoRespuesta) {
			control.printOutput("No se puede desconectar: esperando respuesta");
			control.setError();
			return;
		}
		try {
			MensajeCerrarConexion req = new MensajeCerrarConexion
				(serverDir.getHostAddress(), localhost.getHostAddress());

			log.fine("Solicitando cierre de conexión...");
			output.writeObject(req);
			statusLock.lock();
			status.esperandoRespuesta = true;
			statusLock.unlock();
			OS.join();
		} catch (IOException | InterruptedException e) {
			System.err.println("CLIENT:");
			System.err.println(e);
		}
	}

	public void onConnectionConfirmed() {
		statusLock.lock();
		status.esperandoRespuesta = false;
		status.conectado = true;
		statusLock.unlock();
		control.printOutput("Conectado");
	}

	public void onCloseConfirmed() {
		statusLock.lock();
		status.esperandoRespuesta = false;
		status.conectado = false;
		statusLock.unlock();
		control.printOutput("Desconectado");
	}

	public void onListConfirmed(MensajeConfirmacionLista m) {
		statusLock.lock();
		status.esperandoRespuesta = false;
		statusLock.unlock();
		usuarios.clear();
		ficherosRemotos.clear();
		for(Usuario u : m.usuarios) {
			usuarios.put(u.iden, u);
		}

		for(Usuario u : m.usuarios) {
			for(String s : u.ficheros) {	
				ArrayList<String> ls;
				if (ficherosRemotos.contains(s)) {
					ls = ficherosRemotos.get(s);
				} else {
					ls = new ArrayList<String>();
				}
				ls.add(u.iden);
				ficherosRemotos.put(s, ls);
			}
		}

		control.printOutput("Lista cargada");
	}

	public void onEmitirFichero(MensajeEmitirFichero m) {
		Path path = Paths.get(fileDir,m.fichero);
		int port;
		do {
			port = 33000 + (int)(Math.random() * (1000));
		} while(conexiones.containsKey(port));
		Emisor em = new Emisor(path, m.usuario, port, this);
		conexiones.put(port, em);
		control.printOutput("Solicitud de emisión recibida:");
		control.printOutput(m.fichero + " -> " + m.usuario.iden + ":" + port);
		statusLock.lock();
		++status.emitiendo;
		statusLock.unlock();
		em.start();
		MensajePreparadoCS resp = new MensajePreparadoCS(
			serverDir.getHostAddress(), localhost.getHostAddress(),
			m.usuario, port, m.fichero);
		try {
			output.writeObject(resp);
		} catch(IOException e) {
			log.log(Level.SEVERE, "Error: ", e);
		}
	}

	public void onPreparadoSC(MensajePreparadoSC m) {
		Path path = Paths.get(fileDir, m.fichero);
		Receptor re = new Receptor(path, m.usu, m.puerto, this);
		conexiones.put(port, re);
		control.printOutput("Recepción de " + m.fichero+ " preparada");
		statusLock.lock();
		++status.recibiendo;
		status.esperandoRespuesta = false;
		statusLock.unlock();
		re.start();
	}

	public void onFinConexion(Conexion e) {
		conexiones.remove(e.getPuerto());
		if (e.getTipo() == 1) {
			// Emisor
			statusLock.lock();
			--status.emitiendo;
			statusLock.unlock();
			control.printOutput("Emisión en " + e.getPuerto() + " finalizada.");
		} else {
			// Receptor
			statusLock.lock();
			--status.recibiendo;
			statusLock.unlock();
			control.printOutput("Recepción de " + e.getFilename() + " finalizada.");
			ficherosLock.lock();
			ficheros.add(e.getFilename());
			ficherosLock.unlock();
			enviarActualizar();
		}
	}

	public Status getStatus() {
		Status res;
		statusLock.lock();
		res = new Status(status);
		statusLock.unlock();
		return res;
	}

	public ConcurrentMap<String,Usuario> getUsuarios() {
		return usuarios;
	}

	public List<String> getFicheros() {
		ficherosLock.lock();
		ArrayList<String> fich = new ArrayList<String>(ficheros);
		ficherosLock.unlock();
		return ficheros;
	}
	
	public ConcurrentMap<String,ArrayList<String>> getFicherosRemotos() {
		return ficherosRemotos;
	}

	public ConcurrentMap<Integer,Conexion> getConexiones() {
		return conexiones;
	}
}
