package client;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

import proto.Usuario;

public class ControladorCliente {

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
		"Comandos básicos:\n" +
		"s: Salir         a: Ayuda       \n" +
		"<Enter>: Actualizar             \n" +
		"\n" +
		"################################\n" +
		"%s" +
		"################################\n" +
		"\n" +
		"Introduzca un comando: ";

	private static final String HELP = 
		"Escriba un comando y pulse\n" +
		"<Enter>. Pulsar <Enter> sin\n" +
		"escribir nada actualiza la\n" +
		"interfaz.\n" +
		"Comandos:\n" +
		"c: Conectar      d: Desconectar \n" +
		"p: Pedir fichero s: Salir       \n" +
		"l: Cargar lista de usuarios     \n" +
		"u: Ver lista de usuarios        \n" +
		"f: Ver lista de ficheros locales\n" +
		"r: Ver lista de ficheros remotos\n" +
		"i: Ver conexiones actuales      \n" +
		"h: Ver historial de mensajes    \n" +
		"<Enter>: Actualizar interfaz    \n" +
		"" +
		"";

	private static final String HELP_MENU = 
		HEADER +
		HELP +
		"\nPulse <Enter> para volver al menú: ";

	private InetAddress servDir, localhost;
	private int port;
	private String id, filesDir, statusStr;

	private final int OUTPUT_BUFF_SIZE = 128;
	private String[] output = new String[OUTPUT_BUFF_SIZE];
	private int outputIndex = 0, totalOutput = 0;
	private boolean error = false;

	private BufferedReader reader;

	private Cliente cliente;
	private Cliente.Status status;

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
		this.servDir = servDir;
		this.localhost = localhost;
		this.port = port;
		this.id = id;
		this.filesDir = filesDir;
		cliente = new Cliente(servDir, port, id, filesDir, this);
		for(int i = 0; i < OUTPUT_BUFF_SIZE; ++i)
			output[i] = "";
	}

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
					case "l":
						printOutput("Cargando lista de usuarios...");
						cliente.listaUsuarios();
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

	private void mostrarListaUsuarios() throws IOException {
		ArrayList<String> usu = new ArrayList<String>(cliente.getUsuarios().keySet());
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

	private void mostrarUsuario(String iden) throws IOException {
		clearScreen();
		System.out.println(HEADER);
		Usuario usu = cliente.getUsuarios().get(iden);
		System.out.println(usu.toString());
		System.out.println("\nPulse <Enter> para volver");
		reader.readLine();
	}

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

	private void mostrarFicheros() throws IOException {
		List<String> fich = cliente.getFicheros();
		mostrarLista(fich, 0);
	}

	private void mostrarFicherosRemotos() throws IOException {
		List<String> fich = new ArrayList<String>(cliente.getFicherosRemotos().keySet());
		mostrarLista(fich, 0);
	}

	private void mostrarHistorial() throws IOException {
		ArrayList<String> hist = new ArrayList<String>(totalOutput);
		int i;
		if (totalOutput < OUTPUT_BUFF_SIZE)
			i = 0;
		else
			i = outputIndex+1;
		for(; i != outputIndex; i = (i+1) % OUTPUT_BUFF_SIZE)
		   hist.add(output[i]);
		mostrarLista(hist, Math.max(0,outputIndex-10));
	}

	void updateStatus() {
		status = cliente.getStatus();
		StringBuilder statusSB = new StringBuilder();
		if (status.conectado)
			statusSB.append("Conectado");
		else
			statusSB.append("Desconectado");

		if (status.esperandoRespuesta)
			statusSB.append(" | Esperando");

		if (status.emitiendo)
			statusSB.append(" | Emitiendo");

		if (status.recibiendo)
			statusSB.append(" | Recibiendo");

		if (error)
			statusSB.append(" | Error");
		statusStr = statusSB.toString();
	}

	public static void clearScreen() {  
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

	public void printOutput(String s) {
		output[outputIndex] = s;
		outputIndex = (outputIndex + 1) % OUTPUT_BUFF_SIZE;
		if(totalOutput < OUTPUT_BUFF_SIZE) ++totalOutput;
	}

	public void setError() {
		error = true;	
	};
}

