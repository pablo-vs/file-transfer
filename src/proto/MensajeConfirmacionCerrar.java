package proto;

public class MensajeConfirmacionCerrar extends Mensaje {

	public MensajeConfirmacionCerrar(String origen, String destino) {
		super(TipoMensaje.MENSAJE_CONFIRMACION_CERRAR_CONEXION, origen, destino);
	}
}
