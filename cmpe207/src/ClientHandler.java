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
			while (socket == null){ //TODO redundant while loop? 
				System.out.println("Client Handler "+number +":\t\tWaiting for new socket");
				QuePack info = server.get_socket(); //since this method contains wait?
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
		System.out.println("Client Handler "+number + " listening on socket: " + socket);
		byte[] buffer = new byte[BUFFERSIZE];
		
		while (true) {						//get COMMAND - (MSG*) and/or WHO
			String whos;
			buffer = new byte[BUFFERSIZE]; 	//clear buffer
			net_in.read(buffer);			//get content from client
			System.out.println("Client Handler " + number+":\t\t" + uname + " wrote to server: " + new String(buffer).trim());
			String input = new String(buffer).trim();
			String command = get_command(input);
			
			if (command != null) {
				switch (command) {
					case "CLOSE": close_connection(); return;
					case "MSG": handle_msg( get_message(input), get_username(input)); break;
					case "SHOW": show_wall( get_username(input) ); break;
					
					default: System.out.println("Client Handler "+ number +" -> recieved unknown command!"); break;
				}
			} else {
				System.out.println("Client Handler "+ number +" -> No information in package from client!");
			}
//			catch (NullPointerException e) {
//				System.out.println("Client Handler "+ number +" -> No information in package from client!");
//			}
			
		}
	}
	private void deliver(Message[] messages) {

		for (Message m : messages) {
			try {
				net_out.write(m.to.getBytes());
				net_out.write(m.message.getBytes());
				net_out.write(m.from.getBytes());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
	}

	/**
	 * TODO fault handeling
	 * Parse input and get the message based on predefined protocol
	 * @param input		the entire message from client
	 * @return			the extracted message
	 */
	private String get_message(String input) {
		boolean write = false;
		String msg = "";
		char[] char_m = input.toCharArray();
		for (char c : char_m) {
			if (write == true) {
				if (c == '\"') 
					return msg;
				else 
					msg += c;
			}
			if (c == '\"') {			
				write = true;
			}
		}
		return null;
	}

	private String get_username(String input) {
		String[] dataRows = input.split(" ");  
		System.out.println("Conserning uname: " +dataRows[dataRows.length-1]);
		return dataRows[dataRows.length-1]; 
	}

	/***
	 * Validate username and connect to database to retrieve messages belonging to that username.
	 * If no username given show users own wall 
	 * @param username
	 * 
	 */
	private void show_wall(String username) {
		// TODO Auto-generated method stub
		System.out.println(username);
		if (username == null) {
			username = this.uname;
		}

		if (server.check_if_user_exist(username)) { //the user exist, get msg's
			Message[] messages = server.get_messages(username);
			deliver(messages);
			for (Message m : messages) {
				System.out.println(m);
			}
		} else {
			//let client know there is no such user
		}
	}

	private void handle_msg(String message, String to) {
		System.out.println("Client Handler "+number +" ->Handle message \"" + message + "\" To: " + to );
		
		//Temporary check due to database limitations //should be tested clientside
		if (message.length() > 160)
			return;
		server.store_message(message, uname, to);
		
		//read the message, verify content. Check uname and recipient... store in db and mark unread. Let server notify recipient.
	}
	
	
	private String get_command(String input) {
		String[] res = input.split(" ", 2);
		return res[0];
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
		String[] first_split = input.split(" ", 2); //first word [0], rest [1]
		if (first_split[0].length() > 0)
			result[0] = first_split[0];
		else {
			result[0] = null;
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
