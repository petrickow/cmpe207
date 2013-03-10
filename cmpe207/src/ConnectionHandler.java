/**
 * Connection Handler, requires Java 1.7
 * Listens for new incoming connections and closes existing connections
 * 
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ConnectionHandler implements Runnable {
	
	Server server;
	ServerSocket ls;
	public int i = 1;
	
	ConnectionHandler(Server server, ServerSocket ls) {
		this.server = server;
		this.ls = ls;
	}
	@Override
	public void run() {
		
		System.out.println("CONHAN:\tListening for new connections");
		Socket newSocket;
		
		while (true) {
			try {
				newSocket = ls.accept();
				System.out.println("Got a new connection!");
					test(newSocket);
			} catch (IOException e) {
				e.printStackTrace();
				//restart thread?
			}
		}
	}
	
	void test(Socket s) throws IOException {
		int BUFFERSIZE = 120;
		InputStream net_in = s.getInputStream();
		OutputStream net_out = s.getOutputStream();
		byte[] recv = new byte[BUFFERSIZE];
		int len;
		while (true) {
			len = net_in.read(recv);
			String uname = new String(recv).trim();
			if (!server.find_user(uname)) {
				System.out.println("No such user");
				net_out.write("No such user".getBytes(), 0, 12); 
				//Create user???
			}
			else {
				server.addConnection(s, uname);
			}
		}

	}
	String parse_recv(String recv) {
		
		return recv;
	}
}
