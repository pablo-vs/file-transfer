package proto;

public class MensajeConfirmacionLista extends Mensaje {
	public final Usuario[] usuarios;

	public MensajeConfirmacionLista(String origen, String destino, Usuario ... usu) {
		super(TipoMensaje.MENSAJE_CONFIRMACION_LISTA_USUARIOS, origen, destino);
		usuarios = new Usuario[usu.length];
		for(int i = 0; i < usu.length; ++i)
			usuarios[i] = usu[i];
	}
}
