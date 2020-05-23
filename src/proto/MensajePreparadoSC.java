package proto;

import java.net.InetAddress;

public class MensajePreparadoSC extends Mensaje {
	public final InetAddress dirIp;
	public final int puerto;

	MensajePreparadoSC(String origen, String destino, InetAddress dir, int p) {
		super(TipoMensaje.MENSAJE_PREPARADO_SERVIDORCLIENTE, origen, destino);
		dirIp = dir;
		puerto = p;
	}
}
