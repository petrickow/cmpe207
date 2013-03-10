import java.net.Socket;


public class CMPE207server {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//TODO - arguments, get max connections, port... any other?
		int port, maxConnections;
		
		
		Server server = new Server(2, 7612);
		server.runServer();

	}

}
