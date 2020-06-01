package client;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

import java.util.concurrent.locks.ReentrantLock;

import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

import proto.Usuario;
import util.Concurrent;

public class ControladorCliente {

	// UI templates
	private static final String HEADER = 
		"################################\n" +
		"##### File Transfer Client #####\n" +
		"################################\n\n";

	private static final String MAIN_MENU =
		HEADER +
		"ID usuario: %s\n" +
		"Servidor: %s\n" +
		"Puerto: %s\n" +
		"Localhost: %s\n" +
		"Estado: %s\n" +
		"\n" +
		"################################\n" +
		"%s" +
		"################################\n" +
		"\n" +
		"Comandos básicos:\n" +
		"c: Conectar      p: Pedir fich. \n" +
		"s: Salir         a: Ayuda       \n" +
		"<Enter>: Actualizar             \n" +
		"\n" +
		"Introduzca un comando: ";

	private static final String HELP = 
		"Escriba un comando y pulse\n" +
		"<Enter>. Pulsar <Enter> sin\n" +
		"escribir nada actualiza la\n" +
		"interfaz.\n" +
		"\n" +
		"Comandos:\n" +
		"c: Conectar      d: Desconectar \n" +
		"p: Pedir fichero s: Salir       \n" +
		"l: Cargar Lista de usuarios     \n" +
		"t: AcTualizar ficheros locales  \n" +
		"u: Ver lista de Usuarios        \n" +
		"f: Ver lista de Ficheros locales\n" +
		"r: Ver lista de ficheros Remotos\n" +
		"x: Ver coneXiones actuales      \n" +
		"h: Ver Historial de mensajes    \n" +
		"" +
		"";

	private static final String HELP_MENU = 
		HEADER +
		HELP +
		"\nPulse <Enter> para volver al menú: ";

	private static String OS = System.getProperty("os.name").toLowerCase();




	private InetAddress servDir, localhost;
	private int port;
	private String id, filesDir, statusStr;

	private final int OUTPUT_BUFF_SIZE = 128;
	private String[] output = new String[OUTPUT_BUFF_SIZE];
	private int outputIndex = 0, totalOutput = 0;
	private boolean error = false;

	private ReentrantLock outputLock = new ReentrantLock(true);

	private BufferedReader reader;

	private Cliente cliente;
	private Cliente.Status status;

	private static Logger log = Logger.getLogger("CONTROLADOR_CLIENTE");
	private static Handler logH;


	// Método Main del cliente. Acepta cuatro argumentos:
	// dirección y puerto del servidor, identificador
	// del usuario y directorio de archivos compartidos
	public static void main(String [] args) {

		BufferedReader reader =
			new BufferedReader(new InputStreamReader(System.in));

		InetAddress servDir, localhost;
		int port;
		String id, filesDir;
		ControladorCliente control;

		if(args.length < 4)
			System.out.println(HEADER);

		try {
			try {
				if (args.length > 0) {
					servDir = InetAddress.getByName(args[0]);
				} else {
					System.out.print("Introduzca la dirección del servidor: ");
					servDir = InetAddress.getByName(reader.readLine());
				}
				localhost = InetAddress.getLocalHost();
			} catch	(UnknownHostException e) {
				System.err.println("Error: Unknown host");
				return;
			}

			if (args.length > 1) {
				port = Integer.parseInt(args[1]);
			} else {
				System.out.print("Introduzca el número de puerto: ");
				port = Integer.parseInt(reader.readLine());
			}

			if (args.length > 2) {
				id = args[2];
			} else {
				System.out.print("Introduzca su identificador de usuario: ");
				id = reader.readLine();
			}

			if (args.length > 3) {
				filesDir = args[3];
			} else {
				System.out.print("Introduzca el directorio de archivos compartidos: ");
				filesDir = reader.readLine();
			}

			control =
				new ControladorCliente(servDir, localhost, port, id, filesDir);
		} catch (IOException e) {
			System.err.println("Input error");
			System.err.println(e);
			return;
		}


		control.run();
	}

	public ControladorCliente(InetAddress servDir, InetAddress localhost,
			int port, String id, String filesDir) throws IOException {
		if (logH == null) {
			logH = new ConsoleHandler();
			log.addHandler(logH);
		}
		this.servDir = servDir;
		this.localhost = localhost;
		this.port = port;
		this.id = id;
		this.filesDir = filesDir;
		cliente = new Cliente(servDir, port, id, filesDir, this);
		for(int i = 0; i < OUTPUT_BUFF_SIZE; ++i)
			output[i] = "";
	}


	// Método principal del controlador. Lee
	// el estado del cliente, escribe el menú, espera un comando
	// y llama al método adecuado
	public void run() {

		reader = new BufferedReader(new InputStreamReader(System.in));

		try {
			while(true) {
				updateStatus();
				updateUi();
				String cmd = reader.readLine();
				boolean commandExecuted = true;
				boolean hadError = error;
				error = false;
				switch(cmd) {
					case "a":
						clearScreen();
						System.out.println(HELP_MENU);
						reader.readLine();
						break;
					case "c":
						printOutput("Conectando...");
						cliente.connect();
						break;
					case "s":
						printOutput("Saliendo...");
						updateUi();
						cliente.disconnect();
						return;
					case "d":
						printOutput("Desconectando...");
						cliente.disconnect();
						break;
					case "p":
						pedirFichero();
						break;
					case "l":
						printOutput("Cargando lista de usuarios...");
						cliente.listaUsuarios();
						break;
					case "t":
						cliente.actualizarFicheros();
						break;
					case "u":
						mostrarListaUsuarios();
						break;
					case "f":
						mostrarFicheros();
						break;
					case "r":
						mostrarFicherosRemotos();
						break;
					case "x":
						mostrarConexiones();
						break;
					case "h":
						mostrarHistorial();
						break;
					case "":
						commandExecuted = false;
						break;
					default:
						commandExecuted = false;
						printOutput("Comando no reconocido.");
				}
				if (!commandExecuted && hadError)
					error = true;
			}
		} catch(IOException e) {
			System.err.println(e);
		}
	}

	// Submenú de lista de usuarios
	// Muestra la lista por páginas y permite ver los
	// detalles de cada usuario
	private void mostrarListaUsuarios() throws IOException {

		// Obtiene la lista de usuarios del cliente

		Concurrent<HashMap<String, Usuario>> lista = cliente.getUsuarios();
		ArrayList<String> usu = new ArrayList<String>(lista.lockAndGet().keySet());
		lista.unlock();
		int index = 0;
		boolean keep = true;
		String out = "";

		while(keep) {

			clearScreen();
			System.out.println(HEADER);

			for(int i = index; i < index+10 && i < usu.size(); ++i) {
				System.out.printf("%s. %s\n", i+1, usu.get(i));
			}
			System.out.printf("\np: previo\tn: siguiente\n<num>: mostrar usuario <num>\nv: volver\n%s\nIntroduzca un comando: ", out);
			out = "";

			String cmd = reader.readLine();
			switch(cmd) {
				case "n":
					if (index+10 < usu.size())
						index += 10;
					break;
				case "p":
					if (index-10 > 0)
						index -= 10;
					else
						index = 0;
					break;
				case "v":
					keep = false;
					break;
				default:
					try {
						int num = Integer.parseInt(cmd);
						if (num > 0 && num <= usu.size())
							mostrarUsuario(usu.get(num-1));
						else
							out = "Usuario desconocido";
					} catch  (NumberFormatException e) {
						out = "Comando no reconocido";						
					}
			}
		}
	}

	// Submenú de usuario
	private void mostrarUsuario(String iden) throws IOException {
		clearScreen();
		System.out.println(HEADER);

		Concurrent<HashMap<String, Usuario>> lista = cliente.getUsuarios();
		Usuario usu = lista.lockAndGet().get(iden);
		lista.unlock();

		System.out.println(usu.toString());
		System.out.println("\nPulse <Enter> para volver");
		reader.readLine();
	}

	// Método de utilidad para mostrar listas genéricas
	private void mostrarLista(List<String> lista, int start) throws IOException {
		int index = start;
		boolean keep = true;
		String out = "";

		while(keep) {

			clearScreen();
			System.out.println(HEADER);

			for(int i = index; i < index+10 && i < lista.size(); ++i) {
				System.out.printf("%s. %s\n", i+1, lista.get(i));
			}
			System.out.printf("\np: previo\tn: siguiente\nv: volver\n%s\nIntroduzca un comando: ", out);
			out = "";

			String cmd = reader.readLine();
			switch(cmd) {
				case "n":
					if (index+10 < lista.size())
						index += 10;
					break;
				case "p":
					if (index-10 > 0)
						index -= 10;
					else
						index = 0;
					break;
				case "v":
					keep = false;
					break;
				default:
					out = "Comando no reconocido";
			}
		}
	}

	// Muestra la lista de ficheros locales
	private void mostrarFicheros() throws IOException {
		List<String> fich = cliente.getFicheros();
		mostrarLista(fich, 0);
	}

	// Muestra la lista de ficheros remotos
	private void mostrarFicherosRemotos() throws IOException {
		Concurrent<HashMap<String, ArrayList<String>>> lista =
			cliente.getFicherosRemotos();
		List<String> fich = new ArrayList<String>(lista.lockAndGet().keySet());
		lista.unlock();
		mostrarLista(fich, 0);
	}

	// Muestra la lista de conexiones
	private void mostrarConexiones() throws IOException {
		Concurrent<HashMap<Integer, Conexion>> lista =
			cliente.getConexiones();
		ArrayList<String> conex = new ArrayList<String>();
		HashMap<Integer, Conexion> map = lista.lockAndGet();
		System.out.println(String.valueOf(map.size()));
		map.forEach((k,v) -> conex.add(v.print()));
		lista.unlock();
		mostrarLista(conex, 0);
	}

	// Muestra el historial de mensajes de salida del controlador
	private void mostrarHistorial() throws IOException {
		ArrayList<String> hist = new ArrayList<String>(totalOutput);
		int i, start;
		if (totalOutput < OUTPUT_BUFF_SIZE) {
			i = 0;
			start = Math.max(0, outputIndex-10);
		}
		else {
			i = outputIndex+1;
			start = OUTPUT_BUFF_SIZE-11;
		}
		for(; i != outputIndex; i = (i+1) % OUTPUT_BUFF_SIZE)
		   hist.add(output[i]);
		mostrarLista(hist, start);
	}

	// Submenú de pedir fichero
	private void pedirFichero() throws IOException {
		clearScreen();
		System.out.println(HEADER);
		System.out.println("Introduce el nombre del fichero (o '.' para cancelar) : ");
		String cmd = reader.readLine();
		if (!cmd.equals("."))
			cliente.pedirFichero(cmd);
	}

	// Lee el estado del cliente y construye
	// un string que lo represente
	void updateStatus() {
		status = cliente.getStatus();
		StringBuilder statusSB = new StringBuilder();
		if (status.conectado)
			statusSB.append("Conectado");
		else
			statusSB.append("Desconectado");

		if (status.esperandoRespuesta)
			statusSB.append(" | Esperando");

		if (status.emitiendo > 0)
			statusSB.append(" | Emitiendo");

		if (status.recibiendo > 0)
			statusSB.append(" | Recibiendo");

		if (error)
			statusSB.append(" | Error");
		statusStr = statusSB.toString();
	}

	public static void clearScreen() {
		if (isWindows())
			System.out.println("\n\n");
		else
			System.out.print("\033[H\033[2J");  
		System.out.flush();  
	}  

	public void updateUi() {
		StringBuilder out = new StringBuilder();
		int i = (OUTPUT_BUFF_SIZE+outputIndex-4) % OUTPUT_BUFF_SIZE;
		for(; i != outputIndex; i = (i+1) % OUTPUT_BUFF_SIZE) {
		   out.append(output[i]);
		   out.append("\n");
		}
		clearScreen();
		System.out.printf(MAIN_MENU, id, servDir, port, localhost,
				statusStr, out.toString());
	}

	// Escribe una línea en la "caja" de mensajes del menú principal
	// que se hará visible la próxima vez que se actualize la interfaz
	// Este método puede ser llamado desde varios hilos
	public void printOutput(String s) {
		log.fine(s);
		outputLock.lock();
		output[outputIndex] = s;
		outputIndex = (outputIndex + 1) % OUTPUT_BUFF_SIZE;
		if(totalOutput < OUTPUT_BUFF_SIZE) ++totalOutput;
		outputLock.unlock();
	}

	public void setError() {
		error = true;	
	};

	private static boolean isWindows() {
		return (OS.indexOf("win") >= 0);
	}
}

