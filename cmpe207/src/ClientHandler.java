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
import java.net.SocketException;
import java.net.SocketTimeoutException;

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
				try {
					socket.setSoTimeout(30*1000); //30 seconds timeout
				} catch (SocketException e) {
					//Timer was interrupted
					e.printStackTrace();
				}
				uname = info.uname; //getting username should be done here to avoid bottleneck in connection handler
				System.out.println("CLIENT HANDLER "+number +":\t\tManaging connection for "+uname);
			}
			try {
				net_in = socket.getInputStream();
				net_out = socket.getOutputStream();
				listen_for_connection();
			} catch (IOException e) {
				System.out.println(e.getLocalizedMessage());
				if (socket.isClosed());
					close_connection();
			} 
			
		}
	}

	//temporary unused solution until we have a check in the protocol to test if client process is running
	private boolean is_alive() {
		return !socket.isClosed();
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
		}
	}
	
	//deliver single message to the user on the other end
	public void deliver(Message message) {
		if (message != null) { 
			Message[] m = new Message[1];
			m[0] = message;
			deliver(m);
		}
	}
	
	//deliver array of messages
	private void deliver(Message[] messages) {
		if (messages != null) {
			for (Message m : messages) {
				try {
					net_out.write(m.to.getBytes());
					net_out.write(m.from.getBytes());
					net_out.write(m.message.getBytes());
					//when success, update Database "read" yes
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	/***
	 * Show all messages to a given user. Receive a username from connection
	 * and query database for messages to that username
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
		String username = new String(wallowner).trim();
		if (server.check_if_user_exist(username)) { //the user exist, get msg's
			Message[] messages = server.get_messages(username);

			deliver(messages);
				
				//debug
			if (messages != null) {
				for (Message m : messages) {
					System.out.println(m);
				}
			}
		} else {
			//let client know there is no such user
			send_error("no such user");
		}
	}

	private void handle_msg() {
		System.out.print("CLIENT HANDLER "+number +" ->Handle message from: " + uname);
		byte[] byte_to = new byte[MAXUSERNAMELENGTH];
		byte[] byte_message = new byte[1024]; //max size for messages is set to be 1024 at the moment
		int length;
		String to, message;
		try {
			//get the recipient and the message itself
			length = net_in.read(byte_to);
			to = new String(byte_to).trim();
			length = net_in.read(byte_message);
			message = new String(byte_message).trim();
			
			//ready to store
			if (server.store_message(message, uname, to)) {
				net_out.write("Message sent".getBytes());
				System.out.println(" to: " + to + " reads: \"" + message + "\"");
			}
			else
				send_error("no such recipient");
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}
	
	private void close_connection() {
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		socket = null;
		server.remove_connection();
	}
	private void send_error(String msg) {
		String error_msg = "ERROR: " + msg;
		try {
			net_out.write(error_msg.getBytes(), 0, error_msg.length());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
