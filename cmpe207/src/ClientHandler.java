/** 
 * Gets the connection and communicates with the client
 * 
 */


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;


public class ClientHandler extends Thread {
	int BUFFERSIZE = 120;

	String uname = "";
	Socket socket; 
	
	public ClientHandler(Socket socket, String uname) {
		this.socket = socket;
		this.uname = uname;
	}
		
	@Override
	public void run() {
		System.out.println("CLIHAN:\tClienthandler for \'" + uname + "\' running");
		try {
			listenForConnection();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//We need to be able to send while still listening for activity
	private void listenForConnection() throws IOException {
		InputStream net_in = socket.getInputStream();
		OutputStream net_out = socket.getOutputStream();
		byte[] buffer = new byte[BUFFERSIZE];
		while (true) {
			net_in.read(buffer);
			
			
			
		}
	}
}
