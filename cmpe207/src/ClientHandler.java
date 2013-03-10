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
	InputStream net_in;
	OutputStream net_out;
	
	public ClientHandler(Socket socket, String uname) {
		this.socket = socket;
		this.uname = uname;
		

	}
		
	@Override
	public void run() {
		
		
		System.out.println("CLIHAN:\tClienthandler for \'" + uname + "\' running");
		try {
			net_in = socket.getInputStream();
			net_out = socket.getOutputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			listen_for_connection();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//We need to be able to send while still listening for activity
	private void listen_for_connection() throws IOException {
		
		byte[] buffer = new byte[BUFFERSIZE];
		while (true) {
			net_in.read(buffer);
			
			
			
		}
	}
	
	synchronized public void new_message(String msg) throws IOException {
		System.out.println(uname + " has gotten a message");
		int len;
		do {
			len = net_in.read(msg.getBytes());
		} while (len != 0);
	}
}
