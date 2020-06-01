package client;

/*
 *	Interfaz para agrupar las clases Emisor y Receptor
 */
public interface Conexion {

	public int getTipo();
	public int getPuerto();
	public String getPeer();
	public String getFilename();

	default public String print() {
		String con1, con2;
		if (getTipo() == 1) {
			con1 = "Emitiendo";
			con2 = "a";
		} else {
			con1 = "Recibiendo";
			con2 = "de";
		}
		return String.format("%s: %s %s %s %s", getPuerto(),
				con1, getFilename(), con2, getPeer());
	}
}
