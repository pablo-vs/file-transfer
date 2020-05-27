package proto;

public class MensajeCerrarConexion extends Mensaje {

	public MensajeCerrarConexion(String origen, String destino) {
		super(TipoMensaje.MENSAJE_CERRAR_CONEXION, origen, destino);
	}
}
