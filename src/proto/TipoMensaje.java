package proto;

public enum TipoMensaje {
	CONEXION, CONFIRMACION_CONEXION, LISTA_USUARIOS,
	CONFIRMACION_LISTA_USUARIOS, PEDIR_FICHERO, EMITIR_FICHERO,
	PREPARADO_CLIENTESERVIDOR, PREPARADO_SERVIDORCLIENTE,
	CERRAR_CONEXION, CONFIRMACION_CERRAR_CONEXION;
}
