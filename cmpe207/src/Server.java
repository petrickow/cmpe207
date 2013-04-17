import java.net.*;
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
	//temp until we have DB running

	LinkedList<String> users;
	
	/*Constructor with max connections*/
	Server(int maxConnections, int port) {
		this.maxConnections = maxConnections;
		this.serverPort = port;
		active_connections = 0;
        
		try {
            listeningSocket = new ServerSocket(serverPort);
        }
        catch (IOException e){
            e.printStackTrace(System.err);
        }
		
		connections = new ClientHandler[maxConnections];
		
		queue = new LinkedList<QuePack>();
		users = new LinkedList<String>();
				
		connection_handler = new Thread(new ConnectionHandler(this, listeningSocket));
	}

	private ResultSet connect_to_database() {
		dbconnection = database_connection();
		if (dbconnection == null) { //failed to connect to database
			return null;
		} else {
			System.out.println("DB connected: " + dbconnection);
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
		connection_handler.start();
		for (int i = 0; i < maxConnections; ++i) {
			connections[i] = new ClientHandler(this, i+1);
			connections[i].start();
		}
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
		active_connections++;
		return queue.pop();
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
		else
			return 0;
	}
	
	/**
	 * Counts down on active connections and notifies all waiting threads
	 */
	synchronized void remove_connection() {
		active_connections--;
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
		try {
			result = dbstat.executeQuery("SELECT name FROM users WHERE name like \""+uname+"\"");
			
			if (result != null) {
				boolean r = result.next();
				close_database_connection();
				result.close();
				return r;
			}
			else {
				return false;
			}
		}
		catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Checks connection and makes sure the connection is live. If not terminate and remove connection.
	 * @param oldh - the connection in question
	 */
//	private boolean check_connection(ClientHandler oldh) {
//		// make sure isClosed does what we are looking for: that is
//		// make sure no one is listening on the other end (make a TEST command in protocol?)
//		
//		return (!oldh.socket.isClosed());
//	}
	
	private Connection database_connection() {
		try {
			Class.forName("com.mysql.jdbc.Driver");	
			//TODO, hardcoded address... prompt user for info for increased security :)
			return ( DriverManager.getConnection("jdbc:mysql://localhost/cmpe207", "root", "root"));
		} catch (Exception e) {
			System.out.println("ERROR: " + e);
		}
		return null;
	}
	
	synchronized Message[] get_messages(String username) {
		String count_query = "SELECT COUNT(uname) FROM messages WHERE uname like \"" + username + "\"";
		ResultSet result = connect_to_database();
		Message[] messages = null;
		try {
			result = dbstat.executeQuery(count_query);
//			int columns = result.getMetaData().getColumnCount();
//			StringBuilder message = new StringBuilder();
//			while (result.next()) {
//				for (int i = 1; i <= columns; i++) {
//					message.append(result.getString(i));
//				}
//			}
			result.first();
			int count = result.getInt(1);	//buuu hard coded
			messages = new Message[count];	//but we get the number of messages so we can constuct array
			System.out.println("Retriving "+ username + " that has " + count + " messages");
			if (count == 0)
				return null;
			else {
				//get the messages:
				String message_query = "SELECT uname, message, sender FROM messages WHERE uname like \"" + username + "\"";
				result = dbstat.executeQuery(message_query);
				int i = 0;
				while (result.next()) {
					messages[i++] = new Message(result.getString("message"), result.getString("uname"), result.getString("sender"));
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
	synchronized boolean store_message(String message, String from, String to) {
		
		connect_to_database();
		String insert = "INSERT INTO messages (uname, message, sender) VALUES (\""+ to + "\", \"" + message +"\", \"" + from+"\")";
		try {
			if (dbstat.executeUpdate(insert) > 0)
				System.out.println("Success");
			else 
				System.out.println("Fail");
		} catch (SQLException e) {
			e.printStackTrace();
			close_database_connection();
			return false;
		}
		
		return true;
	}
	
}
