package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.io.IOException;

import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

import proto.Usuario;
import util.Concurrent;

public class Servidor {

	private int port;
	private static Logger log = Logger.getLogger("SERVER");
	private static Handler logH;

	private Concurrent<HashMap<String, Usuario>> usuarios =
		new Concurrent<>(new HashMap<>());
	private Concurrent<HashMap<String, OyenteCliente>> conexiones =
		new Concurrent<>(new HashMap<>());
	private Concurrent<HashMap<String, ArrayList<String>>> ficheros =
		new Concurrent<>(new HashMap<>());

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
		HashMap<String, Usuario> usu = usuarios.lockAndGet();
		res = !(usu.containsKey(user.iden));
		if(res)
			usu.put(user.iden, user);
		usuarios.unlock();
		if(res) {
			HashMap<String, OyenteCliente> conex = conexiones.lockAndGet();
			conex.put(user.iden, OC);
			conexiones.unlock();

			log.finer("Archivos nuevos:" + user.ficheros.size());
			log.finer(String.join(",", user.ficheros));
			HashMap<String, ArrayList<String>> fich = ficheros.lockAndGet();
			for(String s : user.ficheros) {	
				ArrayList<String> ls;
				if (fich.containsKey(s)) {
					ls = fich.get(s);
				} else {
					ls = new ArrayList<String>();
				}
				ls.add(user.iden);
				fich.put(s, ls);
			}
			ficheros.unlock();
		}
		return res;
	}

	public boolean removeUsuario(Usuario user) {
		boolean res = false;
		HashMap<String, Usuario> usu = usuarios.lockAndGet();
		res = usu.containsKey(user.iden);
		if (res)
			usu.remove(user.iden);
		usuarios.unlock();
		if(res) {
			HashMap<String, OyenteCliente> conex = conexiones.lockAndGet();
			conex.remove(user);
			conexiones.unlock();
			log.finer("Archivos antiguos:" + user.ficheros.size());
			log.finer(String.join(",", user.ficheros));
			HashMap<String, ArrayList<String>> fich = ficheros.lockAndGet();
			for(String s : user.ficheros) {	
				ArrayList<String> ls = fich.get(s);
				if(ls.size() > 1) {
					ls.remove(user.iden);
					fich.replace(s, ls);
				} else {
					fich.remove(s);
				}
			}
			ficheros.unlock();
		}
		return res;
	}

	public void actualizarUsuario(Usuario user) {
		log.fine("Actualizando usuario " + user.iden);
		HashMap<String, Usuario> usu = usuarios.lockAndGet();
		Usuario act = new Usuario(usu.get(user.iden));
		usu.replace(user.iden, user);
		usuarios.unlock();
		
		ArrayList<String> oldFiles = new ArrayList<String>(act.ficheros);

		log.finer("Archivos antiguos: " + act.ficheros.size());
		log.finer(String.join(",", oldFiles));

		log.finer("Archivos nuevos:" + user.ficheros.size());
		log.finer(String.join(",", user.ficheros));

		oldFiles.removeAll(user.ficheros);
		log.finer("Archivos eliminados:");

		HashMap<String, ArrayList<String>> fich = ficheros.lockAndGet();

		for(String s : oldFiles) {
			log.finer(s);
			ArrayList<String> ls = fich.get(s);
			if(ls.size() > 1) {
				ls.remove(user.iden);
				fich.replace(s, ls);
			} else {
				fich.remove(s);
			}
		}

		ArrayList<String> newFiles = new ArrayList<String>(user.ficheros);
		newFiles.removeAll(act.ficheros);
		log.finer("Archivos nuevos:");
		for(String s : newFiles) {	
			log.finer(s);
			ArrayList<String> ls;
			if (fich.containsKey(s)) {
				ls = fich.get(s);
			} else {
				ls = new ArrayList<String>();
			}
			ls.add(user.iden);
			fich.put(s, ls);
		}
		ficheros.unlock();
	}

	public Usuario[] getListaUsuarios() {
		Usuario[] res;
		HashMap<String, Usuario> usu = usuarios.lockAndGet();
		res = usu.values().toArray(new Usuario[0]);
		usuarios.unlock();
		return res;
	}
	
	public List<String> consultarFichero(String fich) {
		List<String> res = ficheros.lockAndGet().get(fich);
		ficheros.unlock();
		return res;
	}

	public OyenteCliente getOyente(String iden) {
		OyenteCliente res = conexiones.lockAndGet().get(iden);
		conexiones.unlock();
		return res;
	}
}
