/**
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
				if (server.addConnection(newSocket)) {
					System.out.println(newSocket);
					InputStream test = newSocket.getInputStream();
					OutputStream testO = newSocket.getOutputStream();
					byte[] recv = new byte[1024];
					int len;
					while (true) {
						len = test.read(recv);
						System.out.println(new String(recv, 0, len));
						
						testO.write(recv, 0, len); //echo
					}
					
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			//wait for 
		}

	}

}
