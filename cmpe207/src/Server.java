import java.net.*;
import java.util.LinkedList;

import java.io.*;

//set it up, wait for connections, spawn clienthandlers for each connection
public class Server {
	static int serverPort = 0;
	static int maxConnections;
	LinkedList<ClientHandler> connections;
	Thread conHan;
	ServerSocket listeningSocket;
	
	//temp until we have DB running
	LinkedList<String> users;
	/*Constructor with max connections*/
	Server(int maxConnections, int port) {
		this.maxConnections = maxConnections;
		this.serverPort = port;
		listeningSocket = null;
        
		try {
            listeningSocket = new ServerSocket(serverPort);
        }
        catch (IOException e){
            e.printStackTrace(System.err);
        }
		connections = new LinkedList<ClientHandler>();
		users = new LinkedList<String>();
		conHan = new Thread(new ConnectionHandler(this, listeningSocket));
		
		
		users.add("bob1");
		users.add("bob2");
		users.add("bob3");
		users.add("bob4");


	}

	int runServer() {
		conHan.start();
		
		System.out.println("Server set up and ready to recieve connections");
		
		return 0;
	}
	
	int addConnection(Socket newSocket, String uname) {
		
		//Make sure the username does not already have a connection
		for (ClientHandler old_handler : connections) {
			if (old_handler.uname.equals(uname)) {	//if we find a connection corresponding to that name
				if (check_connection(old_handler)); //and it is still active return false
					return -2;						//CONNECTION ALREADY IN USE
			}
		}
		ClientHandler h = new ClientHandler(newSocket, uname);
		
		if (connections.size() > maxConnections) 	//return false if we already have 10 connections
			return -1;								//TO MANY CONNECTIONS
		else {
			h.start();
			connections.add(h);
			return 0;
		}
	}
	
	boolean removeConnection(String uname) {
		
		return false;
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


	
	synchronized boolean find_user(String uname) {

		return users.contains(uname);
	}
}
