import java.net.*;
import java.util.LinkedList;
import java.io.*;

//set it up, wait for connections, spawn clienthandlers for each connection
public class Server {

	int active_connections;
	int serverPort;
	int maxConnections;
	
	ClientHandler connections[];
	Thread connection_handler;
	ServerSocket listeningSocket;
	
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
		users = new LinkedList<String>(); //TODO dbconnection
		
		connection_handler = new Thread(new ConnectionHandler(this, listeningSocket));
		
		users.add("bob1");
		users.add("bob2");
		users.add("bob3");
		users.add("bob4");
	}

	int runServer() {
		connection_handler.start();
		for (int i = 0; i < maxConnections; ++i) {
			connections[i] = new ClientHandler(this);
			connections[i].start();
		}
		System.out.println("Server set up and ready to recieve connections");
		
		return 0;
	}

	synchronized QuePack get_socket() {
		while (queue.size() == 0) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		active_connections++;
		return queue.pop();
	}
	
	synchronized int addConnection(Socket newSocket, String uname) {
		//Make sure the username does not already have a connection
		for (ClientHandler old_handler : connections) {
			if (old_handler.uname != null) {
				if (old_handler.uname.equals(uname) /*&& check_connection(old_handler)*/) {	//if we find a connection corresponding to that name
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
	synchronized void removeConnection() {
		active_connections--;
		notifyAll();			//wake sleeping threads
	}
	
	/**
	 * Checks connection and makes sure the connection is live. If not terminate and remove connection.
	 * @param oldh - the connection in question
	 */
	private boolean check_connection(ClientHandler oldh) {
		// TODO Auto-generated method stub, make sure isClosed does what we are looking for: that is
		// * make sure no one is listening on the other end (make a TEST command in protocol)
		
		return (!oldh.socket.isClosed());
	}
	
	/**
	 * Checks if we have a user by given name
	 * @param uname
	 * @return
	 */
	synchronized boolean find_user(String uname) {

		return users.contains(uname);
	}
}
