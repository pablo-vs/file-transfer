package proto;

public class MensajeCerrarConexion extends Mensaje {

	MensajeCerrarConexion(String origen, String destino) {
		super(TipoMensaje.MENSAJE_CERRAR_CONEXION, origen, destino);
	}
}
