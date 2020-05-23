package proto;

public class MensajeConexion extends Mensaje {
	private String[] ficheros;

	MensajeConexion(String origen, String destino, String ... fich) {
		super(TipoMensaje.MENSAJE_CONEXION, origen, destino);
		ficheros = new String[fich.length];
		for(int i = 0; i < fich.length; ++i)
			ficheros[i] = fich[i];
	}
}
