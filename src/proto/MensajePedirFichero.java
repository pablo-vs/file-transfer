package proto;

public class MensajePedirFichero extends Mensaje {
	public final String fichero;

	public MensajePedirFichero(String origen, String destino, String fich) {
		super(TipoMensaje.MENSAJE_PEDIR_FICHERO, origen, destino);
		fichero = fich;
	}
}
