/**
 * Connection Handler, requires Java 1.7
 * Listens for new incoming connections and closes existing connections
 * 
 * Not very scalable, if we get tons of connectionrequests this thread will be overworked
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
				connect_client(newSocket);
			} catch (IOException e) {
				e.printStackTrace();
				//restart thread?
			}
		}
	}
	
	void connect_client(Socket s) throws IOException {
		int BUFFERSIZE = 120;
		InputStream net_in = s.getInputStream();
		OutputStream net_out = s.getOutputStream();
		byte[] recv = new byte[BUFFERSIZE];
		int len;

		len = net_in.read(recv);						//get uname !!! protocol must be defined
		String uname = new String(recv).trim();
		if (!server.find_user(uname)) {
			System.out.println("No such user");
			send_error(net_out, "No such user"); 
			//Create user???
		}
		else {
			switch (server.addConnection(s, uname)) {
				case 0: send_ack(net_out); System.out.println("connection made"); break;
				case -1: send_error(net_out, "TO MANY CONNECTIONS"); System.out.println("TO MANY CONNECTIONS"); break; //try again? blocking!
				case -2: send_error(net_out, "ALREADY SIGNED IN"); System.out.println("ALREADY SIGNED IN"); break;
				default: System.out.println("Whoot? >__<"); break;
			}
		}
	}
	
	private void send_ack(OutputStream net_out) throws IOException {
		net_out.write("ACK".getBytes(), 0, 3);
	}

	private void send_error(OutputStream net_out, String msg) throws IOException {
		String error_msg = "ERROR " + msg;
		net_out.write(error_msg.getBytes(), 0, error_msg.length());
	}
}
