package proto;

public class MensajeActualizar extends Mensaje {
	public final Usuario usuario;

	public MensajeActualizar(String origen, String destino, Usuario usu) {
		super(TipoMensaje.MENSAJE_ACTUALIZAR, origen, destino);
		usuario = usu;
	}
}
