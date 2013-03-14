import java.net.Socket;

public class QuePack implements Comparable<QuePack> {
	Socket s;
	String uname;
	public QuePack(Socket s, String uname) {
		this.s = s;
		this.uname = uname;
	}
	@Override
	public int compareTo(QuePack pack) {
		return uname.compareTo(pack.uname);
	}
}