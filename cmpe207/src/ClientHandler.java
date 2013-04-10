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
				System.out.println("Client Handler:\t\tWaiting for new socket");
				QuePack info = server.get_socket();
				socket = info.s;
				uname = info.uname;
				System.out.println("Client Handler:\t\tManaging connection for "+uname);
			}
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
		System.out.println(socket);
		byte[] buffer = new byte[BUFFERSIZE];
		while (true) {
			buffer = new byte[BUFFERSIZE]; //zero out buffer
			net_in.read(buffer); //Y U NO BLOCK
			System.out.println("Client Handler:\t\t" + uname + " wrote to server: " + new String(buffer).trim());
			String command = get_command(new String(buffer).trim());
			try {
				switch (command) {
					case "CLOSE": close_connection(); return;
					case "MSG": handle_msg(); break;
					case "SHOW": show_wall(get_parameters(new String(buffer).trim())); break;
					
					default: System.out.println("unknown command!"); break;
				}
			
			} catch (NullPointerException e) {
				System.out.println("No information in package from client!");
			}
			
		}
	}
	
	/***
	 * Validate username and connect to database to retrieve messages belonging to that username.
	 * If no username given show users own wall 
	 * @param username
	 */
	private void show_wall(String username) {
		// TODO Auto-generated method stub
		
	}

	private String get_parameters(String buffer) {
		// TODO Auto-generated method stub
		return null;
	}

	private void handle_msg() {
		System.out.println("Handle message from " + uname);
		//read the message, verify content. Check uname and recipient... store in db and mark unread. Let server notify recipient.
		
	}
	/** 
	 * The command should be the first word in the literal string received from client
	 * @param recv
	 * @return command
	 */
	private String get_command(String recv) {
		String[] split;
		
		split = recv.split("\\s+");
		if (split[0].length() > 0)
			return split[0]; //return input
		else
			return null;
	}
	
	private void close_connection() throws IOException {
		socket.close();
		socket = null;
		server.removeConnection();
	}

	@SuppressWarnings("unused")
	private synchronized void new_message(String msg) throws IOException {
		System.out.println("Client Handler:\t\t" + uname + " has gotten a message");
		int len;
		do {
			len = net_in.read(msg.getBytes());
		} while (len != 0);
	}
}
