package proto;

import java.net.InetAddress;

public class MensajePreparadoCS extends Mensaje {
	public final Usuario usuario;
	public final int puerto;
	public final String fichero;

	public MensajePreparadoCS(String origen, String destino, Usuario usu, int p, String fich) {
		super(TipoMensaje.MENSAJE_PREPARADO_CLIENTESERVIDOR, origen, destino);
		usuario = new Usuario(usu);
		puerto = p;
		fichero = fich;
	}
}
