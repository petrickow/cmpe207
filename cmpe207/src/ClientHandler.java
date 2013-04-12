/** 
 * Gets the connection and communicates with the client
 * 
 */


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ClientHandler extends Thread {
	int BUFFERSIZE = 120;

	Server server;
	String uname;
	Socket socket;
	InputStream net_in;
	OutputStream net_out;
	int number;
	public ClientHandler(Server server, int number) {
		this.server = server;
		this.number = number;
	}
		
	@Override
	public void run() {
		while(true) {
			while (socket == null){
				System.out.println("Client Handler "+number +":\t\tWaiting for new socket");
				QuePack info = server.get_socket();
				socket = info.s;
				uname = info.uname;
				System.out.println("Client Handler "+number +":\t\tManaging connection for "+uname);
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
		System.out.println("Client Handler "+number + " " + socket);
		byte[] buffer = new byte[BUFFERSIZE];
		while (true) {
			buffer = new byte[BUFFERSIZE]; 	//clear buffer
			net_in.read(buffer);			//get content from client
			System.out.println("Client Handler " + number+":\t\t" + uname + " wrote to server: " + new String(buffer).trim());
			String[] command = parse_input(new String(buffer).trim());
			String u = uname;
			if (command[2] != null)
				u = command[2];
			try {
				switch (command[0]) {
					case "CLOSE": close_connection(); return;
					case "MSG": handle_msg(); break;
					case "SHOW": show_wall( u ); break;
					
					default: System.out.println("Client Handler "+ number +" -> recieved unknown command!"); break;
				}
			
			} catch (NullPointerException e) {
				System.out.println("Client Handler "+ number +" -> No information in package from client!");
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
		System.out.println(username);
		if (username == null) {
			username = this.uname;
		}
		
		//the user exist, get info
		if (server.check_if_user_exist(username)) {
			Message[] messages = server.get_messages(username);
			for (Message m : messages) {
				System.out.println(m);
			}
		} else {
			//let client know there is no such user
		}
		
		
		
	}

	private void handle_msg() {
		System.out.println("Client Handler "+number +" ->Handle message from " + uname);
		//read the message, verify content. Check uname and recipient... store in db and mark unread. Let server notify recipient.
		
	}
	/** 
	 * The command should be the first word in the literal string received from client
	 * @param recv
	 * @return command
	 */
	private String[] parse_input(String input) { //TODO TODO TODO TODO get message and sender user name
		String[] result = new String[3];
		//return[0] == command
		//return[1] == msg
		//return[2] == username
		String[] first_split = input.split(" ", 2);
		if (first_split.length > 0)
			result[0] = first_split[0];
		else {
			result[0] = null;
			return result;
		}
		return result;
	}
	
	
	private void close_connection() throws IOException {
		socket.close();
		socket = null;
		server.remove_connection();
	}

	@SuppressWarnings("unused")
	private synchronized void new_message(String msg) throws IOException {
		System.out.println("Client Handler "+number +":\t\t" + uname + " has gotten a message");
		int len;
		do {
			len = net_in.read(msg.getBytes());
		} while (len != 0);
	}
}
