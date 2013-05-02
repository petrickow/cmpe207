/** 
 * Gets the connection and communicates with the client
 * 
 */

//VERY IMPORTANT TODO!
//Timeout for all network operations (net_in/out)!

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collection;

public class ClientHandler extends Thread {
	int BUFFERSIZE = 1024;
	int MAXUSERNAMELENGTH = 30;
	boolean NEW_MESSAGES = true;
	boolean ALL_MESSAGES = false;
	Server server;
	String uname;
	Socket socket = null;
	BufferedReader net_in;
	PrintStream net_out;
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
				if (info != null) {
					socket = info.s;
					try {
						socket.setSoTimeout(30*1000); //n seconds timeout
					} catch (SocketException e) {
						//Socket error! 
						e.printStackTrace();
						return;
					}
					uname = info.uname; //getting username should be done here to avoid bottleneck in connection handler
					System.out.println("CLIENT HANDLER "+number +":\t\tManaging connection for "+uname);
				}
			}
				
			//first get and send all new messages
			try {
				net_out = new PrintStream(socket.getOutputStream());
				net_in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				deliver(server.get_messages(uname, true), true); //TODO , mark messages as read
				listen_for_connection();
			} catch (IOException e) {
				System.out.println("timeout, make sure connection is alive");
				if (!is_alive()) {
					close_connection();
				}
			}
		}
	}

	//temporary unused solution until we have a check in the protocol to test if client process is running
	private boolean is_alive() {
		int i = 0;
		if (socket == null) {
			return false;
		}
		
		while (i < 3) { //Try three times if it times out, but close if return null, see api
			write_client("TEST\n");
			String r = read_client();
			if (r == null) { //returns null when no contact, no need to try again 
				return false;
			}
			
			if (r != null && r.trim().equals("ALIVE")) {
				return true;
			}
			else {
				i++;
				System.out.print("***");
			}
		}
		System.out.println("");
		return false;
	}

	//We need to be able to send while still listening for activity
	private void listen_for_connection() throws IOException {
		System.out.println("CLIENT HANDLER "+number + " listening on socket: " + socket);
		
		while (true) {						//get COMMAND - (MSG*) and/or WHO
			
			System.out.println("...waiting for client\t" + is_alive());
			socket.setSoTimeout(10 * 1000);
			String input = read_client();	//get content from client
			if (input != null) 
				System.out.println("CLIENT HANDLER " + number+":\t\t" + uname + " wrote to server: " + input);
			else {
				if (!is_alive()) {
					System.out.println("Lost connection");
					close_connection();
					return;
				}
			}
			
			String command = input /*get_command(input)*/;
			
			if (command != null) {
				switch (command) {
					case "CLOSE": close_connection(); return;
					case "MSG": handle_msg(); break;
					case "SHOW": show_wall(read_client()); break;
					case "LIST": deliver(server.get_users()); break;
					default: System.out.println("CLIENT HANDLER "+ number +" -> recieved unknown command!"); break;
				}
			} else {
				if (!is_alive()) {
					System.out.println("CLIENT HANDLER "+ number +" -> Socket suddenly closed!");
					close_connection();
				} else {
					//bogus package
				}
			}
		}
	}
	
	//deliver single message to the user on the other end
	public void deliver(Message message, boolean mark) {
		if (message != null) { 
			Message[] m = new Message[1];
			m[0] = message;
			deliver(m, mark);
		}
	}
	
	//deliver array of messages
	private void deliver(Message[] messages, boolean mark) {
		Integer id_delivered[];
		if (messages != null) {
			id_delivered = new Integer[messages.length];
			int i = 0;
			
			for (Message m : messages) {
				write_client(m.to + "\n");
				write_client(m.from + "\n");
				write_client(m.message + "\n");
//				write_client(m.msgid + "\n");
				id_delivered[i] = new Integer(m.msgid);
			}
			if (mark)
				mark_messages(id_delivered);
		}
		write_client("LAST\n");		
	}
	
	private void deliver(Collection<User> users) {
		if (users != null) {
			for (User u : users) {
				write_client(u.getName() +"\n");
				if (u.getStatus())
					write_client("yes\n");
				else
					write_client("no\n");
			}
		}
		write_client("LAST\n");
	}
	public void alert_asynch() {
		write_client("MSG\n");
	}

	public void mark_messages(Integer[] msgid) {
		server.mark_read(msgid);
	}
	
	/***
	 * Show all messages to a given user. Receive a username from connection
	 * and query database for messages to that username
	 */
	private void show_wall(String username) {
		if (username == null)
			return;
		
		boolean only_new = false; 					//to make code more self explanatory
		if (server.check_if_user_exist(username)) { //the user exist, get msg's
			System.out.println("... getting "+ username + "'s wall");

			//TODO, move nullpointertest here to avoid doing the same test over
			Message[] messages = server.get_messages(username, only_new);

			deliver(messages, false);
				
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
		String to, message;
		//get the recipient and the message itself
		to = read_client();
		message = read_client();
		
		//ready to store
		if (server.store_message(message, uname, to)) {
			write_client("SENT\n");
			System.out.println(" to: " + to + " reads: \"" + message + "\"");
		}
		else
			send_error("no such recipient");
	}
	
	private void close_connection() {
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		socket = null;
		server.remove_connection(uname);
	}
	
	private void send_error(String msg) {
		String error_msg = "ERROR: " + msg + "\n";
		write_client(error_msg);
	}
	
	private void write_client(String s) {
		if (s != null) {
			try {
				net_out.write(s.getBytes());
				net_out.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private String read_client() {
		String s;
		try {
			s = net_in.readLine();
			if (s == null) {
//				System.out.println("got null, client closed?");
				return null;
			}
			System.out.println("server received " + s);
		} catch (IOException e) {
			e.getLocalizedMessage();
			//timeout?
			return null;
		}
		
		return s;
	}
	
	@SuppressWarnings("unused")
	private void error_shutdown() {
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void test() {
		write_client("TEST\n");
		System.out.println("test sent");
	}
}
