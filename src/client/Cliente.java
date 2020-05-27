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

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.Handler;
import java.util.logging.ConsoleHandler;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import proto.*;

public class Cliente {

	private InetAddress serverDir, localhost;
	private int port;
	private String iden, fileDir;
	private OyenteServidor OS;
	private ObjectOutputStream output;
	private ObjectInputStream input;
	private ControladorCliente control;

	private Logger log = Logger.getLogger("CLIENTE");
	private Handler logH = new ConsoleHandler();

	private ReentrantLock statusLock = new ReentrantLock(true);

	private ArrayList<String> ficheros;

	private ConcurrentHashMap<String, Usuario> usuarios =
		new ConcurrentHashMap<String, Usuario>();

	private ConcurrentHashMap<String, ArrayList<String>> ficherosRemotos =
		new ConcurrentHashMap<String, ArrayList<String>>();


	public class Status {
		public boolean conectado = false, 
			esperandoRespuesta = false,
			emitiendo = false,
			recibiendo = false;

		public Status() {}
		public Status(Status st) {
			conectado = st.conectado;
			esperandoRespuesta = st.esperandoRespuesta;
			emitiendo = st.emitiendo;
			recibiendo = st.recibiendo;
		}
	}

	private Status status = new Status();

	public Cliente(InetAddress dir, int p, String id, String fd,
		   	ControladorCliente cont) throws IOException {
		log.addHandler(logH);
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
		
		ficheros = new ArrayList<String>(
			Files.list(Paths.get(fd))
			.map((x) -> x.getFileName().toString())
			.collect(Collectors.toList()));

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

	public void onPreparadoSC(MensajePreparadoSC m) {
		Receptor rec;
		control.printOutput("Solicitud de emisión recibida:");
		control.printOutput(m.fichero + " -> " + m.usuario.iden);
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
		return ficheros;
	}
	
	public ConcurrentMap<String,ArrayList<String>> getFicherosRemotos() {
		return ficherosRemotos;
	}
}
