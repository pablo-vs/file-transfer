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

import java.util.concurrent.locks.ReentrantReadWriteLock;

import proto.Usuario;

public class Servidor {

	private int port;
	private Logger log = Logger.getLogger("SERVER");
	private Handler logH = new ConsoleHandler();

	private HashSet<Usuario> usuarios;
	private ReentrantReadWriteLock lockUsuarios = new ReentrantReadWriteLock(true);

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
		log.addHandler(logH);
		usuarios = new HashSet<Usuario>();

		Usuario u = new Usuario("yuza", InetAddress.getLoopbackAddress(), new ArrayList<String>());
		usuarios.add(u);
		u = new Usuario("yatta", InetAddress.getLoopbackAddress(), new ArrayList<String>());
		usuarios.add(u);
		u = new Usuario("omake", InetAddress.getLoopbackAddress(), new ArrayList<String>());
		usuarios.add(u);
		u = new Usuario("mazaa", InetAddress.getLoopbackAddress(), new ArrayList<String>());
		usuarios.add(u);
		u = new Usuario("halah", InetAddress.getLoopbackAddress(), new ArrayList<String>());
		usuarios.add(u);
		u = new Usuario("haram", InetAddress.getLoopbackAddress(), new ArrayList<String>());
		usuarios.add(u);
		u = new Usuario("caram", InetAddress.getLoopbackAddress(), new ArrayList<String>());
		usuarios.add(u);
		u = new Usuario("colon", InetAddress.getLoopbackAddress(), new ArrayList<String>());
		usuarios.add(u);
		u = new Usuario("yatta2", InetAddress.getLoopbackAddress(), new ArrayList<String>());
		usuarios.add(u);
		u = new Usuario("omake2", InetAddress.getLoopbackAddress(), new ArrayList<String>());
		usuarios.add(u);
		u = new Usuario("mazaa2", InetAddress.getLoopbackAddress(), new ArrayList<String>());
		usuarios.add(u);
		u = new Usuario("halah2", InetAddress.getLoopbackAddress(), new ArrayList<String>());
		usuarios.add(u);
		u = new Usuario("haram2", InetAddress.getLoopbackAddress(), new ArrayList<String>());
		usuarios.add(u);
		u = new Usuario("caram2", InetAddress.getLoopbackAddress(), new ArrayList<String>());
		usuarios.add(u);
		u = new Usuario("colon2", InetAddress.getLoopbackAddress(), new ArrayList<String>());
		usuarios.add(u);
		u = new Usuario("yatta3", InetAddress.getLoopbackAddress(), new ArrayList<String>());
		usuarios.add(u);
		u = new Usuario("omake3", InetAddress.getLoopbackAddress(), new ArrayList<String>());
		usuarios.add(u);
		u = new Usuario("mazaa3", InetAddress.getLoopbackAddress(), new ArrayList<String>());
		usuarios.add(u);
		u = new Usuario("halah3", InetAddress.getLoopbackAddress(), new ArrayList<String>());
		usuarios.add(u);
		u = new Usuario("haram3", InetAddress.getLoopbackAddress(), new ArrayList<String>());
		usuarios.add(u);
		u = new Usuario("caram3", InetAddress.getLoopbackAddress(), new ArrayList<String>());
		usuarios.add(u);
		u = new Usuario("colon3", InetAddress.getLoopbackAddress(), new ArrayList<String>());
		usuarios.add(u);

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

	public boolean addUsuario(Usuario user) {
		boolean res = false;
		lockUsuarios.writeLock().lock();
		res = usuarios.add(user);
		lockUsuarios.writeLock().unlock();
		return res;
	}

	public boolean removeUsuario(Usuario user) {
		boolean res = false;
		lockUsuarios.writeLock().lock();
		res = usuarios.remove(user);
		lockUsuarios.writeLock().unlock();
		return res;
	}

	public Usuario[] getListaUsuarios() {
		Usuario[] res;
		lockUsuarios.readLock().lock();
		res = usuarios.toArray(new Usuario[0]);
		lockUsuarios.readLock().unlock();
		return res;
	}

}
