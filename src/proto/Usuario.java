package proto;

import java.net.InetAddress;
import java.util.List;

public class Usuario {
	public final String iden;
	public final InetAddress dir;
	public final List<String> ficheros;

	public Usuario(String id, InetAddress d, List<String> f) {
		iden = id;
		dir = d;
		ficheros = f;
	}


}
