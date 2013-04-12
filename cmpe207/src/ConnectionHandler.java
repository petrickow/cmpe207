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
	
	ConnectionHandler(Server server, ServerSocket ls) {
		this.server = server;
		this.ls = ls;
	}
	@Override
	public void run() {
		System.out.println("Connection Handler:\tListening for new connections");
		Socket newSocket;
		while (true) {
			try {
				newSocket = null;
				newSocket = ls.accept();
				System.out.println("Connection Handler:\tGot a new connection from " + newSocket);
				
				//TODO, this method should be in clienthandler to avoid blocking if the client is slow to respond
				connect_client(newSocket);
				//id not que request.
			} catch (IOException e) {
				e.printStackTrace();
				//restart thread?
			}
		}
	}
	
	void connect_client(Socket s) throws IOException {
		int BUFFERSIZE = 100;
		InputStream net_in = s.getInputStream();
		OutputStream net_out = s.getOutputStream();
		byte[] recv = new byte[BUFFERSIZE];
//		int len;
		/*len = */net_in.read(recv);	//This is a bottleneck, if client f's up before sending stuff, blocking, freeze
		
		String uname = new String(recv).trim();
		
		if (!server.check_if_user_exist(uname)) {
			System.out.println("Connection Handler:\tNo such user" + uname);
			send_error(net_out, "No such user"); 
			s.close();
		}
		else {
			switch (server.addConnection(s, uname)) {
				case 0: send_ack(net_out); System.out.println("Connection Handler:\tConnection success"); break;
				case -1: send_error(net_out, "NACK WAIT"); System.out.println("Connection Handler:\tTO MANY CONNECTIONS, request queued"); break; //put in queue
				case -2: send_error(net_out, "ALREADY SIGNED IN"); System.out.println("Connection Handler:\tALREADY SIGNED IN"); s.close(); break;
				default: System.out.println("Connection Handler:\tWhoot? >__<"); break; //unknown error
			}
		}
	}
	
	private void send_ack(OutputStream net_out) throws IOException {
		net_out.write("ACK".getBytes(), 0, 3);
	}

	private void send_error(OutputStream net_out, String msg) throws IOException {
		String error_msg = "ERROR: " + msg;
		net_out.write(error_msg.getBytes(), 0, error_msg.length());
	}
}
