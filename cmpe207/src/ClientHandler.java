/** 
 * Gets the connection and communicates with the client
 * 
 */

//VERY IMPORTANT TODO!
//Timeout for all network operations (net_in/out)!

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ClientHandler extends Thread {
	int BUFFERSIZE = 120;
	int MAXUSERNAMELENGTH = 30;
	
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
				System.out.println("CLIENT HANDLER "+number +":\t\tWaiting for new socket");
				QuePack info = server.get_socket(); //since this method contains wait?
				socket = info.s;
				uname = info.uname; //getting username should be done here to avoid bottleneck in connection handler
				System.out.println("CLIENT HANDLER "+number +":\t\tManaging connection for "+uname);
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
		System.out.println("CLIENT HANDLER "+number + " listening on socket: " + socket);
		byte[] buffer = new byte[BUFFERSIZE];
		
		while (true) {						//get COMMAND - (MSG*) and/or WHO
			String whos;
			buffer = new byte[BUFFERSIZE]; 	//clear buffer
			net_in.read(buffer);			//get content from client
			System.out.println("CLIENT HANDLER " + number+":\t\t" + uname + " wrote to server: " + new String(buffer).trim());
			String input = new String(buffer).trim();
			
			String command = input /*get_command(input)*/;
			
			if (command != null) {
				switch (command) {
					case "CLOSE": close_connection(); return;
					case "MSG": handle_msg(); break;
					case "SHOW": show_wall(); break;
					
					default: System.out.println("CLIENT HANDLER "+ number +" -> recieved unknown command!"); break;
				}
			} else {
				System.out.println("CLIENT HANDLER "+ number +" -> No information in package from client!");
			}
//			catch (NullPointerException e) {
//				System.out.println("CLIENT HANDLER "+ number +" -> No information in package from client!");
//			}
			
		}
	}
	
	//deliver single message
	public void deliver(Message message) {
		Message[] m = new Message[1];
		m[0] = message;
		deliver(m);
	}
	
	//deliver array of messages
	private void deliver(Message[] messages) {

		for (Message m : messages) {
			try {
				net_out.write(m.to.getBytes());
				net_out.write(m.message.getBytes());
				net_out.write(m.from.getBytes());
				//when success, update Database "read" yes
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

//	/**
//	 * Parse input from client for COMMAND as the first word in received string
//	 * @param input		input from client
//	 * @return			null if error otherwise the command string
//	 */
//	private String get_command(String input) {
//		String[] res = input.split(" ", 2);
//		return res[0];
//	}
//	/**
//	 * TODO fault handling
//	 * Parse input and get the message based on predefined protocol
//	 * @param input		the entire message from client
//	 * @return			the extracted message
//	 */
//	private String get_message(String input) {
//		boolean write = false;
//		String msg = "";
//		char[] char_m = input.toCharArray();
//		for (char c : char_m) {
//			if (write == true) {
//				if (c == '\"') 
//					return msg;
//				else 
//					msg += c;
//			}
//			if (c == '\"') {			
//				write = true;
//			}
//		}
//		return null;
//	}

//	private String get_username(String input) {
//		String[] dataRows = input.split(" ");  
//		System.out.println("Conserning uname: " +dataRows[dataRows.length-1]);
//		return dataRows[dataRows.length-1]; 
//	}

	/***
	 * Validate username and connect to database to retrieve messages belonging to that username.
	 * If no username given show users own wall 
	 * @param username
	 * 
	 */
	private void show_wall() {
		// TODO Auto-generated method stub
		byte[] wallowner = new byte[MAXUSERNAMELENGTH];
		try {
			net_in.read(wallowner);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		String username = new String(wallowner);

		if (server.check_if_user_exist(username)) { //the user exist, get msg's
			Message[] messages = server.get_messages(username);
			deliver(messages);
			for (Message m : messages) {
				System.out.println(m);
			}
		} else {
			//let client know there is no such user
			try {
				net_out.write("ERROR, no such user".getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void handle_msg() {
		System.out.print("CLIENT HANDLER "+number +" ->Handle message from: " + uname);
		byte[] byte_to = new byte[MAXUSERNAMELENGTH];
		byte[] byte_message = new byte[160];
		int length;
		String to, message;
		try {
			//get the recipient and the message itself
			length = net_in.read(byte_to);
			to = new String(byte_to).trim();
			length = net_in.read(byte_message);
			message = new String(byte_message).trim();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		//Temporary check due to database limitations //should be tested clientside
		System.out.println(" to: " + to + " reads: \"" + message + "\"");
		
		if (server.store_message(message, uname, to))
			System.out.println("CLIENT HANDLER "+number +" -> success handling message");
		
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
		System.out.println("CLIENT HANDLER "+number +":\t\t" + uname + " has gotten a message");
		int len;
		do {
			len = net_in.read(msg.getBytes());
		} while (len != 0);
	}
	
	private void error_shutdown() {
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}
