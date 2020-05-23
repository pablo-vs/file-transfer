package proto;

public class MensajeConexion extends Mensaje {
	public final Usuario usuario;

	MensajeConexion(String origen, String destino, Usuario usu) {
		super(TipoMensaje.MENSAJE_CONEXION, origen, destino);
		usuario = usu;
	}
}
