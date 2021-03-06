import java.net.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.io.*;
import java.sql.*;

//set it up, wait for connections, spawn clienthandlers for each connection
public class Server {

	int active_connections;
	int serverPort;
	int maxConnections;
	
	ClientHandler connections[];
	Thread connection_handler;
	
	ServerSocket listeningSocket;
	Connection dbconnection;
	Statement dbstat;
	LinkedList<QuePack> queue;
	
	HashMap<String, User> user_map;
	Shell debugconsole;
	
	/*Constructor with max connections*/
	Server(int maxConnections, int port) {
		this.maxConnections = maxConnections;
		this.serverPort = port;
		active_connections = 0;
	
	}

	private ResultSet connect_to_database() {
		dbconnection = database_connection();
		if (dbconnection == null) { //failed to connect to database
			return null;
		} else {
			try {
				dbstat = dbconnection.createStatement();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	private void close_database_connection() {
		try {
			dbconnection.close();
			dbstat.close();
		}catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	int runServer() {
		connections = new ClientHandler[maxConnections];
		queue = new LinkedList<QuePack>();
		user_map = new HashMap<String, User>();
		load_users();
		try {
            listeningSocket = new ServerSocket(serverPort);
        }
        catch (IOException e){
            e.printStackTrace(System.err);
        }
		
		connection_handler = new Thread(new ConnectionHandler(this, listeningSocket));
		connection_handler.start();
		
		for (int i = 0; i < maxConnections; ++i) {
			connections[i] = new ClientHandler(this, i+1);
			connections[i].start();
		}
		debugconsole = new Shell(this);
		debugconsole.start();
		System.out.println("Server set up and ready to recieve connections");
		
		return 0;
	}

	/**
	 * Method for ClientHandles to request a socket to handle
	 * @return	socket in need of handling 
	 */
	synchronized QuePack get_socket() { 
		while (queue.size() == 0) { //can two threads pass this test at the same time?
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (!queue.peek().s.isClosed()) {
			active_connections++;
			return queue.pop();
		}
		else
			return null;
	}
	
	/**
	 * ConnectionHandler updates connection list when receiving a new connection
	 * @param newSocket - a socket assigned to the new client
	 * @param uname		- user name of the client //move to client handler
	 * @return			- error codes 	-2 connection already in use
	 * 									-1 wait for available client handler
	 * 									0  all OK
	 */
	synchronized int addConnection(Socket newSocket, String uname) {
		//Make sure the username does not already have a connection
		
		for (ClientHandler old_handler : connections) {
			if (old_handler.uname != null) {
				if (old_handler.uname.equals(uname) && old_handler.socket != null) {	//if we find a connection corresponding to that name
					return -2;						//CONNECTION ALREADY IN USE
				}
			}
		}
		
		queue.add(new QuePack(newSocket, uname));
		notifyAll();
		if (active_connections >= maxConnections)
			return -1;
		else {
			System.out.println("whoot, y no uname? "+uname);
			user_map.get(uname).logon();
			return 0;
		}
	}
	
	/**
	 * Counts down on active connections and notifies all waiting threads
	 */
	synchronized void remove_connection(String uname) {
		active_connections--;
		user_map.get(uname).logoff();
		notifyAll();			//wake sleeping threads
	}

	/**
	 * Checks if we have a user by given name
	 * @param uname	the name we want to see if is valid
	 * @return	true if uname checks out
	 * @throws SQLException 
	 */
	synchronized boolean check_if_user_exist(String uname) {
		//TODO use == of sorts instead of like... check mysql doc
		ResultSet result = connect_to_database();
		boolean user_exists = false;
		try {
			result = dbstat.executeQuery("SELECT uname FROM users WHERE uname like \""+uname+"\" ");
			if (result != null) {
				result.beforeFirst();
				user_exists = result.next();
				close_database_connection();
				result.close();
				
			}
			else {
				close_database_connection();
			}
			return user_exists;
		}
		catch (SQLException e) {
			e.printStackTrace();
			return user_exists;
		}
	}
	
	private Connection database_connection() {
		try {
			Class.forName("com.mysql.jdbc.Driver");	
			//TODO, hardcoded address... prompt user for info for increased security :)
			return ( DriverManager.getConnection("jdbc:mysql://localhost/cmpe207", "root", "root"));
		} catch (Exception e) {
			System.out.println("SERVER ERROR: " + e);
		}
		return null;
	}

	synchronized void load_users() {
		
		ResultSet result; 
		connect_to_database();
		String username_query = "SELECT uname FROM users";
		try {
			
			result = dbstat.executeQuery(username_query);
			result.beforeFirst();
			while (result.next()) {
				user_map.put(result.getString(1), new User(result.getString(1)));
			}
			result.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		close_database_connection();
	}
	
	synchronized Collection<User> get_users() {
		if (user_map.size() > 0)  
			return user_map.values();
		else 
			return null;
	}
	
	synchronized boolean get_user_status(String uname) {
		User u = user_map.get(uname);
		if (u == null)
			return false;
		else 
			return u.getStatus();
	}
	
	synchronized Message[] get_messages(String username, boolean only_new) {
		String count_query;
		String message_query;
		if (only_new) {
			message_query = "SELECT uname, message, sender, id FROM messages WHERE uname like \"" + username + "\" AND `isread` IS FALSE";
			count_query = "SELECT COUNT(uname) FROM messages WHERE uname like \"" + username + "\" AND `isread` IS FALSE";
		}
		else {
			message_query = "SELECT uname, message, sender, id FROM messages WHERE uname like \"" + username + "\"";
			count_query = "SELECT COUNT(uname) FROM messages WHERE uname like \"" + username + "\"";
		}
		
		ResultSet result = connect_to_database();
		Message[] messages = null;
		try {
			result = dbstat.executeQuery(count_query); //count number of messages
			result.first();
			int count = result.getInt(1);	//buuu hard coded to get the result from count query
			
			messages = new Message[count];	//but we get the number of messages so we can constuct array
			
			if (count == 0) {
				System.out.println("SERVER:\tNo messages for "+ username);
				return null;
			}
			else {
				//get the messages:
				System.out.println("SERVER:\tRetriving all "+ username + "'s " + count + " messages");
				result = dbstat.executeQuery(message_query);
				int i = 0;
				while (result.next()) {
					messages[i++] = new Message(result.getInt("id"), result.getString("message"), result.getString("uname"), result.getString("sender"));
//					String m = messages[i-1].toString();
				}
			}
			result.close();
			close_database_connection();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return messages;
	}
	
	/**
	 * Save new message to server database. Default marked as not read
	 * @param message	the message text
	 * @param from		which user name has sent the message
	 * @param to		user name of recipient
	 * @return			true if successfully stored, false if there is an error
	 */
	public synchronized boolean store_message(String message, String from, String to) {
		if (message == null || to == null) {
			//either message or to is null
			return false;
		}
		if (!check_if_user_exist(to)) {
			System.out.println("User does not exist");
			return false;
		}
		connect_to_database();
		String insert = "INSERT INTO messages (uname, message, sender) VALUES (\""+ to + "\", \"" + message +"\", \"" + from+"\")";
		String get_id = "SELECT id FROM messages WHERE uname like \""+to +"\" AND message like \""+ message + "\"";
		//TODO, refactor, messy code
		try {
			if (dbstat.executeUpdate(insert) > 0) {
				System.out.println("SERVER:\tSuccess inserting message from " + from + "to " + to + "  into database");
				for (ClientHandler ch : connections) {
					if (ch.uname != null) {
						if (ch.uname.equals(to)) { //special case, do not send to the user who sent it
							ResultSet result = dbstat.executeQuery(get_id);
							if (result.next()) {
								int id = result.getInt("id");
								System.out.println("SERVER:\tthe user is online, delivering message and updating database");
								ch.alert_asynch();
								ch.deliver(new Message(id, message, to, from), true);
							//String read = "UPDATE messages SET isread = true WHERE uname like \""+to+"\" and message like \""+message+"\""; //TODO, test on id
							//dbstat.executeUpdate(read);
								break;
							} 
						}
					}
				}
			}
			else {
				System.out.println("SERVER:\tDatabase insertion fail");
				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			close_database_connection();
			return false;
		}			
		close_database_connection();

		return true;
	}

	public int num_con() {
		return active_connections;
	}
	
	public void mark_read(Integer[] ids) {
		if (ids != null) {
			connect_to_database();
			String read;
			
			for (int id : ids) { 
				read = "UPDATE messages SET isread = true WHERE id ="+id;
				try {
					if (dbstat.executeUpdate(read)>0)
						System.out.println("Success in update of " + id);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			close_database_connection();
		}
	}
	
	public void test_msg() {
		for (ClientHandler h : connections) {
			if (h.socket != null) {
				h.test();
				return;
			}
		}
		System.out.println("Fail");
	}
}
