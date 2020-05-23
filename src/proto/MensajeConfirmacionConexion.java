package proto;

public class MensajeConfirmacionConexion extends Mensaje {

	MensajeConfirmacionConexion(String origen, String destino) {
		super(TipoMensaje.MENSAJE_CONFIRMACION_CONEXION, origen, destino);
	}
}
