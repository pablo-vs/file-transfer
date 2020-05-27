package proto;

import java.io.Serializable;

public abstract class Mensaje implements Serializable {
	private final TipoMensaje tipo;
	private final String origen, destino;

	Mensaje(TipoMensaje t, String o, String d) {
		tipo = t;
		origen = o;
		destino = d;
	}

	public int getTipo() {
		return tipo.ordinal();
	}

	public String getOrigen() {
		return origen;
	}

	public String getDestino() {
		return destino;
	}

}
