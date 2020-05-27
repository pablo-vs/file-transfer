package proto;

public class MensajeListaUsuarios extends Mensaje {

	public MensajeListaUsuarios(String origen, String destino) {
		super(TipoMensaje.MENSAJE_LISTA_USUARIOS, origen, destino);
	}
}
