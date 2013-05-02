import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class Shell extends Thread {
	Client c;
	boolean connected = false;
	
	public Shell(Client c) {
		this.c = c;
	}
	
	public void run() {
		BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
		String s = null;
		while (!connected) {
			try {
				System.out.println("Please enter your username here or \"quit\" to quit: ");
				s = bufferRead.readLine();
				if (s.equals("quit")) {
					c.quit();	//let monitor object know we want to quit
					return;		//end end run()
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			connected = c.connect(s);
			
		}
		

		while (connected) {
			System.out.println("Please enter your commands here or \"quit\" to quit: ");
			try {
				s = bufferRead.readLine();
				if (s.trim().equalsIgnoreCase("quit")) {
					c.quit();
				}
				else {
					c.userCommand(s);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
