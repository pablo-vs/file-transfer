package client;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

import java.util.concurrent.locks.Lock;

import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.ConsoleHandler;

import proto.*;

public class OyenteServidor extends Thread {

	private ObjectInputStream input;
	private ObjectOutputStream output;
	private Cliente cliente;
	private static Logger log = Logger.getLogger("OYENTE_CLIENTE");
	private static Handler logH;

	public OyenteServidor(ObjectInputStream in, ObjectOutputStream out, Cliente cl) {
		if (logH == null) {
			logH = new ConsoleHandler();
			log.addHandler(logH);
		}
		input = in;
		output = out;
		cliente = cl;
	}

	public void run() {
		try {

			TipoMensaje [] posiblesMensajes = TipoMensaje.values();

			while(true) {

				Mensaje recibido = (Mensaje) input.readObject();

				log.fine("Mensaje recibido.");
				log.fine(posiblesMensajes[recibido.getTipo()].name());
				switch(posiblesMensajes[recibido.getTipo()]) {
					case MENSAJE_CONFIRMACION_CONEXION:
						cliente.onConnectionConfirmed();
						break;
					case MENSAJE_CONFIRMACION_LISTA_USUARIOS:
						cliente.onListConfirmed((MensajeConfirmacionLista) recibido);
						break;
					case MENSAJE_EMITIR_FICHERO:
						cliente.onEmitirFichero((MensajeEmitirFichero) recibido);
						break;
					case MENSAJE_PREPARADO_SERVIDORCLIENTE:
						cliente.onPreparadoSC((MensajePreparadoSC) recibido);
						break;
					case MENSAJE_CONFIRMACION_CERRAR_CONEXION:
						cliente.onCloseConfirmed();
						output.close();
						return;
				}
	
			}
		} catch (IOException | ClassNotFoundException e) {
			System.err.println("CLIENT:");
			System.err.println(e);
		}
	}

}
