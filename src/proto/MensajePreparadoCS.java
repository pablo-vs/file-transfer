package proto;

import java.net.InetAddress;

public class MensajePreparadoCS extends Mensaje {
	public final Usuario usuario;
	public final InetAddress dirIp;
	public final int puerto;

	MensajePreparadoCS(String origen, String destino, Usuario usu, InetAddress dir, int p) {
		super(TipoMensaje.MENSAJE_PREPARADO_CLIENTESERVIDOR, origen, destino);
		usuario = usu;
		dirIp = dir;
		puerto = p;
	}
}
