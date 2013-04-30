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
					//TODO handle message					
					System.out.println(fromServer);
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
		String uname;
		if (connected) {
			write_client("LIST\n");
			recv_users();
		}
	}

	public void post(String message) {
		postOn(this.uname, message);
	}

	public void postOn(String name, String message) {
		if (connected) {
			try {
				sockOutput.write("MSG".getBytes());
				sockOutput.write(name.getBytes());
				sockOutput.write(message.getBytes());
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

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
			System.out.format("%30s %s", name, online);
		} 
	}
	
	private void recv_messages() {
		String to, from, msg;
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
		command = s.split("\\s+");
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
			}
		} else if (command.length == 2) {
			switch (command[0]) {
				case "post":
					post(command[1]);
					break;
				case "show":
					showFrom(command[1]);
					break;
			}
		} else if (command.length == 3) {
			switch (command[0]) {
				case "post":
					postOn(command[1], command[2]);
					break;
			}
		}
	}

	private void write_client(String sending) {
		try {
			System.out.println("Sending "+sending);
			sockOutput.write(sending.getBytes());
			sockOutput.flush();
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
