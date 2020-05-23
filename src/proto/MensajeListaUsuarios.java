package proto;

public class MensajeListaUsuarios extends Mensaje {
	public final String[] ficheros;

	MensajeListaUsuarios(String origen, String destino) {
		super(TipoMensaje.MENSAJE_LISTA_USUARIOS, origen, destino);
	}
}
