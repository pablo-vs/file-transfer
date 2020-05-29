package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.io.IOException;

import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.ConcurrentHashMap;

import proto.Usuario;

public class Servidor {

	private int port;
	private static Logger log = Logger.getLogger("SERVER");
	private static Handler logH;

	private ConcurrentHashMap<String, Usuario> usuarios =
		new ConcurrentHashMap<String, Usuario>();
	private ReentrantReadWriteLock lockUsuarios = new ReentrantReadWriteLock(true);
	private ConcurrentHashMap<String, OyenteCliente> conexiones =
		new ConcurrentHashMap<String, OyenteCliente>();

	private ConcurrentHashMap<String, ArrayList<String>> ficheros =
		new ConcurrentHashMap<String, ArrayList<String>>();

	public static void main(String [] args) {
		if (args.length < 1) return;
		int port = Integer.parseInt(args[0]);

		Servidor server = new Servidor(port);
		server.init();
	};

	public Servidor(int p) {
		port = p;
	}

	public void init() {
		if (logH == null) {
			logH = new ConsoleHandler();
			log.addHandler(logH);
		}

		try (ServerSocket serverSocket = new ServerSocket(port)) {

			log.fine("Initializing...");
			while(true) {
				Socket socket = serverSocket.accept();
				log.fine("Received incoming connection.");
				new Thread(new OyenteCliente(socket, this)).start();
			}

		} catch (IOException e) {
			log.log(Level.SEVERE, "Error: ", e);
		}
	}

	public boolean addUsuario(Usuario user, OyenteCliente OC) {
		boolean res = false;
		lockUsuarios.writeLock().lock();
		res = !(usuarios.containsKey(user.iden));
		if(res)
			usuarios.put(user.iden, user);
		lockUsuarios.writeLock().unlock();
		if(res) {
			conexiones.put(user.iden, OC);
			for(String s : user.ficheros) {	
				ArrayList<String> ls;
				if (ficheros.contains(s)) {
					ls = ficheros.get(s);
				} else {
					ls = new ArrayList<String>();
				}
				ls.add(user.iden);
				ficheros.put(s, ls);
			}
		}
		return res;
	}

	public boolean removeUsuario(Usuario user) {
		boolean res = false;
		lockUsuarios.writeLock().lock();
		res = usuarios.containsKey(user.iden);
		if (res)
			usuarios.remove(user.iden);
		lockUsuarios.writeLock().unlock();
		if(res) {
			conexiones.remove(user);
			for(String s : user.ficheros) {	
				ArrayList<String> ls = ficheros.get(s);
				if(ls.size() > 1) {
					ls.remove(user.iden);
					ficheros.put(s, ls);
				} else {
					ficheros.remove(s);
				}
			}
		}
		return res;
	}

	public void actualizarUsuario(Usuario user) {
		log.fine("Actualizando usuario " + user.iden);
		Usuario act = usuarios.get(user.iden);
		ArrayList<String> oldFiles = new ArrayList<String>(act.ficheros);

		log.finer("Archivos antiguos: " + act.ficheros.size());
		log.finer(String.join(",", oldFiles));

		log.finer("Archivos nuevos:" + user.ficheros.size());
		log.finer(String.join(",", user.ficheros));

		oldFiles.removeAll(user.ficheros);
		log.finer("Archivos eliminados:");
		for(String s : oldFiles) {
			log.finer(s);
			ArrayList<String> ls = ficheros.get(s);
			if(ls.size() > 1) {
				ls.remove(user.iden);
				ficheros.put(s, ls);
			} else {
				ficheros.remove(s);
			}
		}
		ArrayList<String> newFiles = new ArrayList<String>(user.ficheros);
		newFiles.removeAll(act.ficheros);
		log.finer("Archivos nuevos:");
		for(String s : newFiles) {	
			log.finer(s);
			ArrayList<String> ls;
			if (ficheros.contains(s)) {
				ls = ficheros.get(s);
			} else {
				ls = new ArrayList<String>();
			}
			ls.add(user.iden);
			ficheros.put(s, ls);
		}
		usuarios.replace(user.iden, user);
	}

	public Usuario[] getListaUsuarios() {
		Usuario[] res;
		lockUsuarios.readLock().lock();
		res = usuarios.values().toArray(new Usuario[0]);
		lockUsuarios.readLock().unlock();
		return res;
	}
	
	public List<String> consultarFichero(String fich) {
		return ficheros.get(fich);
	}

	public OyenteCliente getOyente(String iden) {
		return conexiones.get(iden);
	}
}
