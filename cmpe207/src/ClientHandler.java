/** 
 * Gets the connection and communicates with the client
 * 
 */


import java.net.Socket;
import java.net.SocketAddress;


public class ClientHandler implements Runnable {
	
	String uname = "";
	Socket socket; 
	
	public ClientHandler(Socket socket) {
		this.socket = socket;
	}
		
	@Override
	public void run() {
		// TODO Auto-generated method stub
		System.out.println("CLIHAN:\tClienthandler for \'" + uname + "\' running");
		listenForConnection();
	}

		
	private void listenForConnection() {
	SocketAddress endpoint;
		while (true) {

		}
	}
}
