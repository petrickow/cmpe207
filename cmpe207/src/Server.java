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
		conHan = new Thread(new ConnectionHandler(this, listeningSocket));
	}

	int runServer() {
		conHan.start();
		
		
		System.out.println("Server set up and ready to recieve connections");
		return 0;
	}

	
	synchronized boolean addConnection(Socket newConnection) {
		if (connections.size() > 10) //return false if we already have 10 connections
			return false;
		else
			return connections.add(new ClientHandler(newConnection));
	}
	synchronized boolean removeConnection(String uname) {
		
		return false;
	}
}
