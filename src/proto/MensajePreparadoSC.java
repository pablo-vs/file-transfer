package proto;

import java.net.InetAddress;

public class MensajePreparadoSC extends Mensaje {
	public final Usuario usu;
	public final int puerto;
	public final String fichero;

	public MensajePreparadoSC(String origen, String destino, Usuario u, int p, String fich) {
		super(TipoMensaje.MENSAJE_PREPARADO_SERVIDORCLIENTE, origen, destino);
		usu = u;
		puerto = p;
		fichero = fich;
	}
}
