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
		
		
		users.add("bob");
	}

	int runServer() {
		conHan.start();
		
		System.out.println("Server set up and ready to recieve connections");
		
		return 0;
	}
	
	boolean addConnection(Socket newSocket, String uname) {
		if (connections.size() > 10) //return false if we already have 10 connections
			return false;
		else {
			ClientHandler h = new ClientHandler(newSocket, uname);
			h.start();
			return connections.add(h);
		}
	}
	
	boolean removeConnection(String uname) {
		
		return false;
	}
	
	synchronized boolean find_user(String uname) {

		return users.contains(uname);
	}
}
