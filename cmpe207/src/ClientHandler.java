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

	Server server;
	String uname;
	Socket socket; 
	InputStream net_in;
	OutputStream net_out;
	
	public ClientHandler(Server server) {
		this.server = server;
	}
		
	@Override
	public void run() {
		while(true) {
			while (socket == null){
				QuePack info = server.get_socket();
				socket = info.s;
				uname = info.uname;
			}
			
			System.out.println("CLIHAN:\tClienthandler for \'" + uname + "\' running");
		
			try {
				net_in = socket.getInputStream();
				net_out = socket.getOutputStream();
				listen_for_connection();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	//We need to be able to send while still listening for activity
	private void listen_for_connection() throws IOException {
		
		byte[] buffer = new byte[BUFFERSIZE];
		while (true) {
			net_in.read(buffer);
			System.out.println(uname + " wrote to server: " + new String(buffer));
			if (new String(buffer).trim().equals("close")) {
				break;
			}
		}
		socket.close();
		socket = null;
		server.removeConnection();
	}
	
	synchronized public void new_message(String msg) throws IOException {
		System.out.println(uname + " has gotten a message");
		int len;
		do {
			len = net_in.read(msg.getBytes());
		} while (len != 0);
	}
}
