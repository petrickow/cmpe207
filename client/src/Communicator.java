import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketException;


public class Communicator extends Thread {
	
	private String serverHostname = null;
	private int serverPort = 0;
	private Socket sock = null;
	private BufferedReader sockInput = null;
	private PrintStream sockOutput = null;
	private String uname = null;
	private boolean connected = false;
	private Client client;

	public Communicator(String serverHostname, int serverPort, Client client) {
		this.serverHostname = serverHostname;
		this.serverPort = serverPort;
		this.client = client;
	}
	
	
	public void run() {
		while (!connected) {
			client.waitForConnection();
			if (client.quit) {
				return;
			}
		}
		while(connected) {
			try {
				sock.setSoTimeout(200); //poll every 200 milliseconds, temporary solution
				
				String fromServer = sockInput.readLine();
				if (fromServer != null) {
					switch (fromServer.trim()) {//TODO handle message
						case "MSG": recv_messages(); break;
						case "TEST": write_client("ALIVE\n"); break;
						case "SENT": System.out.println("Message delivered"); break;
					}
				}
			} catch (IOException e ) {
				String com = client.getCommand();
				if (com != null) 
					whatAction(com);
			}
		}
	}
	
	public boolean connect(String uname) {
		this.uname = uname;
		String buffer;
		try {
			sock = new Socket(serverHostname, serverPort);
			System.out.println(sock);
			sockOutput = new PrintStream(sock.getOutputStream());
			sockInput = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			write_client(uname + "\n");
			buffer = sockInput.readLine().trim();
		} catch (IOException e) {
			System.out.println("Host not found/connection refused, check server address/port number");
			return false;
		}
			if (buffer.equals("ACK")) {
				connected = true;
				System.out.println("Connection have been established.");
				recv_messages();
				return true;
			} else if (buffer.equals("NACK WAIT")) {
				int tries = 0;
				
				try {
					sock.setSoTimeout(5*1000);
					while (tries < 3) {
						String resp = sockInput.readLine();
						if (resp.equals("ACK")) { 
							sock.setSoTimeout(0);
							return connected = true;
						} else {
							sock.setSoTimeout(0);
							tries++;
						}
					}
				} catch (IOException e) {
					tries++;
					System.out.print("***");
					
					
				}
				System.out.println("Server is busy, please try later.");
				return false;
			} else if (buffer.startsWith("ERROR:")) { //TODO, make functional check
				System.out.println(buffer);
				return false;
			} else {
				System.out.println("Error when connecting, please check your username and try again.");
				return false;
			}

	}
	
	public void listAll() {
		System.out.println(connected);
		if (connected) {
			write_client("LIST\n");
			recv_users();
		}
	}

	public void post(String message) {
		postOn(this.uname, message);
	}

	public void postOn(String to, String message) {
		if (connected) {
				write_client("MSG\n");
				write_client(to + "\n");
				write_client(message + "\n");
		} else {
			System.out.println("Connection required.");
		}
	}

	public void show() {
		showFrom(this.uname);
	}

	public void showFrom(String name) {

		
		try {
			sock.setSoTimeout(3*1000); //3 second timeout pr message to avoid congestion 
		} catch (SocketException e) {
			System.out.println("Failed assigning timeout to socket");
			return;
		}
	
		if (connected) {
			write_client("SHOW\n");
			write_client(name + "\n");
			recv_messages();

		}
	}
	
	private void recv_users() {
		String name, online;
		
		while (true) {
			name = read_client();
			if (name.equals("ERROR") || name.equals("LAST"))
				return;
			online = read_client();
			System.out.format("%30s %s\n", name, online);
		} 
	}
	
	private void recv_messages() {
		String to, from, msg, id;
		while (true) {				
			to = read_client();
			if (to.equals("ERROR") || to.equals("LAST")) {
				System.out.println("abort read/finished reading");
				try {
					sock.setSoTimeout(0);
				} catch (SocketException e) {
					e.printStackTrace();
				}
				return;
			}
			from = read_client();
			msg = read_client();
//			id = read_client(); //TODO, return id to connection handler to update database for reliable delivery
			String fullmsg = String.format("From %s, To %s\r\n%s", from, to, msg);
			System.out.println(fullmsg);
		}
	}

	public void quit() {
		try {
			sockOutput.write("CLOSE\n".getBytes());
			sockOutput.close();
			sockInput.close();
			sock.close();
			connected = false;
		} catch (IOException e) {
			System.err.println("Exception closing socket.");
			e.printStackTrace(System.err);
		}
	}

	public void whatAction(String s) {
		try {
			sock.setSoTimeout(0);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		String[] command;
		command = s.split("@|>");
		// for(int i = 0; i<command.length; i++){
		// System.out.println(command[i]);
		// }

		if (command.length == 1) {
			switch (command[0]) {
			case "listall":
				listAll();
				break;
			case "show":
				show();
				break;
			case "quit":
				quit();
				return;
			case "help":
				System.out
						.println("Valid commands are listed as following:\r\n"
								+ "\tlistall - to list all users.\r\n"
								+ "\tpost>(message) - to post on your own wall\r\n"
								+ "\tpost@(username)>(message) - to post on other's wall\r\n"
								+ "\tshow - to show your own wall\r\n"
								+ "\tshow@(username) - to show other's wall\r\n"
								+ "\tquit - to logoff and close the program");
				break;
			default:
				System.out
						.println("Error, invalid command. Type \"help\" for valid command and formats.");
			}
		} else if (command.length == 2) {
			switch (command[0]) {
			case "post":
				post(command[1]);
				break;
			case "show":
				showFrom(command[1]);
				break;
			default:
				System.out
						.println("Error, invalid command. Type \"help\" for valid command and formats.");
			}
		} else if (command.length == 3) {
			switch (command[0]) {
			case "post":
				postOn(command[1], command[2]);
				break;
			default:
				System.out
						.println("Error, invalid command. Type \"help\" for valid command and formats.");
			}
		} else {
			System.out
					.println("Error, invalid command. Type \"help\" for valid command and formats.");
		}
	}

	private void write_client(String sending) {
		try {
//			System.out.println("Sending "+sending);
			sockOutput.write(sending.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private String read_client() {
		String recv;
		try {
			recv = sockInput.readLine();
			if (recv.startsWith("ERROR:")) {
				System.out.println(recv);
				return "ERROR";
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		// String s = new String(buffer).trim();
		
		return recv.trim();
	}
}
