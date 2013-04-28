import java.io.*;
import java.net.*;

public class Client {

	private String serverHostname = null;
	private int serverPort = 0;
	private Socket sock = null;
	private BufferedReader sockInput = null;
	private PrintStream sockOutput = null;
	private String uname = null;
	private boolean connected = false;

	public Client(String serverHostname, int serverPort) {
		this.serverHostname = serverHostname;
		this.serverPort = serverPort;
	}

	public void connect(String uname) {
		this.uname = uname;

		try {
			sock = new Socket(serverHostname, serverPort);
			System.out.println(sock);
			sockOutput = new PrintStream(sock.getOutputStream());
			sockInput = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			System.out.println("...");
			sockOutput.write(uname.getBytes());
			// sockOutput.write("\n".getBytes());
			String buffer = sockInput.readLine();

			if (buffer.trim().equals("ACK")) {
				connected = true;
				System.out.println("Connection have been established.");
			} else if (buffer.equals("NACK WAIT")) {
				connected = false;
				System.out.println("Server is busy, please try later.");
			} else {
				connected = false;
				System.out.println("Error when connecting, please check your username and try again.");
			}
		} catch (IOException e) {
			e.printStackTrace(System.err);
			return;
		}

	}

	public void listAll() {
		byte[] byte_uname = null;
		String uname;
		if (connected) {
			try {
				sockOutput.write("LIST".getBytes());
				uname = sockInput.readLine();
				// uname = new String(byte_uname).trim();
				System.out.println(uname);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
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

		String to, from, msg;
		try {
			sock.setSoTimeout(3*1000); //3 second timeout pr message
		} catch (SocketException e) {
			System.out.println("Failed assigning timeout to socket");
			return;
		}
	
		if (connected) {
			write_client("SHOW\n");
			write_client(name + "\n");

			while (true) {				
				to = read_client();
				if (to.trim().equals("LAST")) {
					System.out.println("\nLAST");
					break;
				}
				from = read_client();
				msg = read_client();
				String fullmsg = String.format("From %s, To %s\r\n%s", from, to, msg);
				System.out.println(fullmsg);
			}

		}

	}

	public void quit() {
		try {
			sockOutput.write("CLOSE\n".getBytes());
			sockOutput.close();
			sockInput.close();
			sock.close();
		} catch (IOException e) {
			System.err.println("Exception closing socket.");
			e.printStackTrace(System.err);
		}
	}

	public void runClient() {
		BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
		String s = null;
		String[] command = null;
		while (!connected) {
			try {
				System.out.println("Please enter your username here or \"quit\" to quit: ");
				s = bufferRead.readLine();
				if (s.equals("quit")) {
					return;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			connect(s);
		}

		while (connected) {
			try {
				System.out.println("Please enter your commands here or \"quit\" to quit: ");
				s = bufferRead.readLine();
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
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void write_client(String s) {
		try {
			System.out.println("Sending "+s);
			sockOutput.write(s.getBytes());
			sockOutput.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private String read_client() {
		byte[] buffer = new byte[50];
		String s;
		try {
			s = sockInput.readLine();

			//TODO, check if it is an error message, abort read and return to normal state
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		// String s = new String(buffer).trim();
		
		return s;
	}
	
}