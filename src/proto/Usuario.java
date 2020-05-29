package proto;

import java.net.InetAddress;
import java.util.ArrayList;
import java.io.Serializable;

public class Usuario implements Serializable {
	public final String iden;
	public final InetAddress dir;
	public ArrayList<String> ficheros;

	public Usuario(String id, InetAddress d, ArrayList<String> f) {
		iden = id;
		dir = d;
		ficheros = new ArrayList<String>(f);
	}


	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;

		if (!(o instanceof Usuario))
			return false;

		Usuario u = (Usuario) o;

		return iden.equals(u.iden);
	}

	@Override
	public int hashCode() {
		return iden.hashCode();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Usuario: ");
		sb.append(iden);
		sb.append("\nDirección: ");
		sb.append(dir);
		sb.append("\nLista de ficheros:\n");
		for(String s: ficheros) {
			sb.append("'");
			sb.append(s);
		   	sb.append("', ");
		}
		return sb.toString();
	}
}
