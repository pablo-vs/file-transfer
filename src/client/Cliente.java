package client;

import java.net.Socket;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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

	private Concurrent<ArrayList<String>> ficheros =
		new Concurrent<>(new ArrayList<>());

	private Concurrent<HashMap<Integer, Conexion>> conexiones =
		new Concurrent<>(new HashMap<>());

	private Concurrent<HashMap<String, Usuario>> usuarios =
		new Concurrent<>(new HashMap<>());

	private Concurrent<HashMap<String, ArrayList<String>>> ficherosRemotos =
		new Concurrent<>(new HashMap<>());

	private Concurrent<Status> status = new Concurrent<Status>(new Status());

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
		
		ficheros.set(new ArrayList<String>(
			Files.list(Paths.get(fd))
			.map((x) -> x.getFileName().toString())
			.collect(Collectors.toList())));

	}

	void connect() {
		try {
			Status st = status.lockAndGet();
			if (st.conectado) {
				control.printOutput("No se puede conectar: ya conectado.");
				control.setError();
				status.unlock();
				return;
			}
			if (st.esperandoRespuesta) {
				control.setError();
				control.printOutput("No se puede conectar: esperando respuesta");
				status.unlock();
				return;
			}
			log.fine("Intentando conectar...");
			Socket socket;
			socket = new Socket(serverDir, port);

			output = new ObjectOutputStream(socket.getOutputStream());

			input = new ObjectInputStream(socket.getInputStream());

			OS = new OyenteServidor(input, output, this);
			OS.start();

			Usuario usu = new Usuario(iden, localhost, ficheros.lockAndGet());
			ficheros.unlock();
			MensajeConexion con = new MensajeConexion(
					serverDir.getHostAddress(), localhost.getHostAddress(), usu);

			output.writeObject(con);
			log.fine("Conexión solicitada.");

			st.esperandoRespuesta = true;
			status.unlock();
			
		} catch (IOException e) {
			System.err.println("CLIENT:");
			System.err.println(e);
		} finally {
			if(status.heldByMe())
				status.unlock();
		}
	}

	void listaUsuarios() {
		try {
			Status st = status.lockAndGet();
			if (!st.conectado) {
				control.printOutput("No se puede cargar lista: no conectado.");
				control.setError();
				status.unlock();
				return;
			}
			MensajeListaUsuarios req = new MensajeListaUsuarios
				(serverDir.getHostAddress(), localhost.getHostAddress());

			log.fine("Solicitando lista de usuarios...");
			output.writeObject(req);
			st.esperandoRespuesta = true;
			status.unlock();
		} catch (IOException e) {
			System.err.println("CLIENT:");
			System.err.println(e);
		} finally {
			if(status.heldByMe())
				status.unlock();
		}
	}

	public void actualizarFicheros() {
		control.printOutput("Leyendo lista de ficheros locales...");
		try {
			ficheros.set(new ArrayList<String>(
				Files.list(Paths.get(fileDir))
				.map((x) -> x.getFileName().toString())
				.collect(Collectors.toList())));
			control.printOutput("Lista actualizada");
			enviarActualizar();
		} catch (IOException e) {
			control.printOutput("No se ha podido leer la lista.");
			log.log(Level.SEVERE, "Error: ", e);
		}
	}

	public void enviarActualizar() {
		try {
			Status st = status.lockAndGet();
			if (!st.conectado) {
				log.fine("No se envía mensaje de actualización: no conectado");
				status.unlock();
				return;
			}
			status.unlock();
			Usuario usu = new Usuario(iden, localhost, ficheros.lockAndGet());
			ficheros.unlock();
			MensajeActualizar req = new MensajeActualizar
				(serverDir.getHostAddress(), localhost.getHostAddress(),
				 usu);
			output.writeObject(req);
			log.fine("Actualización enviada al servidor");
		} catch (IOException e) {
			System.err.println("CLIENT:");
			System.err.println(e);
		} finally {
			if(status.heldByMe())
				status.unlock();
		}
	}

	void pedirFichero(String fich) {
		try {
			Status st = status.lockAndGet();
			if (!st.conectado) {
				control.printOutput("No se puede pedir fichero: no conectado.");
				control.setError();
				status.unlock();
				return;
			}
			if(ficheros.lockAndGet().contains(fich)) {
				control.printOutput("Error: Fichero ya existente.");
				control.setError();
				ficheros.unlock();
				status.unlock();
				return;
			}
			ficheros.unlock();
			HashMap<String, ArrayList<String>> fr = ficherosRemotos.lockAndGet();
			if (!fr.containsKey(fich)) {
				control.printOutput("Fichero desconocido");
				control.setError();
				ficherosRemotos.unlock();
				status.unlock();
				return;
			}
			ficherosRemotos.unlock();
			MensajePedirFichero req = new MensajePedirFichero
				(serverDir.getHostAddress(), localhost.getHostAddress(),
				 fich);

			control.printOutput("Pidiendo fichero " + fich);
			output.writeObject(req);
			st.esperandoRespuesta = true;
			status.unlock();
		} catch (IOException e) {
			System.err.println("CLIENT:");
			System.err.println(e);
		} finally {
			if(status.heldByMe())
				status.unlock();
			if(ficherosRemotos.heldByMe())
				ficherosRemotos.unlock();
		}
	}

	void disconnect() {
		try {
			Status st = status.lockAndGet();
			if (!st.conectado) {
				control.printOutput("No se puede desconectar: no conectado.");
				control.setError();
				status.unlock();
				return;
			}
			if (st.esperandoRespuesta) {
				control.printOutput("No se puede desconectar: esperando respuesta");
				control.setError();
				status.unlock();
				return;
			}
			status.unlock();
			MensajeCerrarConexion req = new MensajeCerrarConexion
				(serverDir.getHostAddress(), localhost.getHostAddress());

			log.fine("Solicitando cierre de conexión...");
			output.writeObject(req);
			st = status.lockAndGet();
			st.esperandoRespuesta = true;
			status.unlock();
			OS.join();
		} catch (IOException | InterruptedException e) {
			System.err.println("CLIENT:");
			System.err.println(e);
		} finally {
			if(status.heldByMe())
				status.unlock();
		}
	}

	public void onConnectionConfirmed() {
		Status st = status.lockAndGet();
		st.esperandoRespuesta = false;
		st.conectado = true;
		status.unlock();
		control.printOutput("Conectado");
	}

	public void onCloseConfirmed() {
		Status st = status.lockAndGet();
		st.esperandoRespuesta = false;
		st.conectado = false;
		status.unlock();
		control.printOutput("Desconectado");
	}

	public void onListConfirmed(MensajeConfirmacionLista m) {
		Status st = status.lockAndGet();
		st.esperandoRespuesta = false;
		status.unlock();
		HashMap<String, Usuario> usu = new HashMap<>();
		HashMap<String, ArrayList<String>> fichR = new HashMap<>();
		for(Usuario u : m.usuarios) {
			usu.put(u.iden, u);
		}

		for(Usuario u : m.usuarios) {
			for(String s : u.ficheros) {	
				ArrayList<String> ls;
				ls = new ArrayList<String>();
				ls.add(u.iden);
				fichR.put(s, ls);
			}
		}

		usuarios.set(usu);
		ficherosRemotos.set(fichR);

		control.printOutput("Lista cargada");
	}

	public void onEmitirFichero(MensajeEmitirFichero m) {
		Path path = Paths.get(fileDir,m.fichero);
		HashMap<Integer, Conexion> conex = conexiones.lockAndGet();
		int port;
		do {
			port = 33000 + (int)(Math.random() * (1000));
		} while(conex.containsKey(port));
		Emisor em = new Emisor(path, m.usuario, port, this);
		conex.put(port, em);
		conexiones.unlock();

		control.printOutput("Solicitud de emisión recibida:");
		control.printOutput(m.fichero + " -> " + m.usuario.iden + ":" + port);
		Status st = status.lockAndGet();
		++st.emitiendo;
		status.unlock();
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

		HashMap<Integer, Conexion> conex = conexiones.lockAndGet();
		conex.put(m.puerto, re);
		conexiones.unlock();

		control.printOutput("Recepción de " + m.fichero+ " preparada");

		Status st = status.lockAndGet();
		++st.recibiendo;
		st.esperandoRespuesta = false;
		status.unlock();

		re.start();
	}

	public void onFinConexion(Conexion e) {
		HashMap<Integer, Conexion> conex = conexiones.lockAndGet();
		conex.remove(e.getPuerto());
		conexiones.unlock();

		Status st = status.lockAndGet();
		if (e.getTipo() == 1) {
			// Emisor
			--st.emitiendo;
			control.printOutput("Emisión en " + e.getPuerto() + " finalizada.");
			status.unlock();
		} else {
			// Receptor
			--st.recibiendo;
			control.printOutput("Recepción de " + e.getFilename() + " finalizada.");
			ArrayList<String> fich = ficheros.lockAndGet();
			fich.add(e.getFilename());
			ficheros.unlock();
			status.unlock();
			enviarActualizar();
		}
	}

	public Status getStatus() {
		Status res = new Status(status.lockAndGet());
		status.unlock();
		return res;
	}

	public Concurrent<HashMap<String,Usuario>> getUsuarios() {
		return usuarios;
	}

	public List<String> getFicheros() {
		ArrayList<String> fich = new ArrayList<String>(ficheros.lockAndGet());
		ficheros.unlock();
		return fich;
	}
	
	public Concurrent<HashMap<String,ArrayList<String>>> getFicherosRemotos() {
		return ficherosRemotos;
	}

	public Concurrent<HashMap<Integer,Conexion>> getConexiones() {
		return conexiones;
	}
}
