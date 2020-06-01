package proto;

public class MensajeEmitirFichero extends Mensaje {
	public final String fichero;
	public final Usuario usuario;

	public MensajeEmitirFichero(String origen, String destino, String fich, Usuario usu) {
		super(TipoMensaje.MENSAJE_EMITIR_FICHERO, origen, destino);
		fichero = fich;
		usuario = new Usuario(usu);
	}
}
