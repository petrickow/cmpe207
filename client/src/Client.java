import java.util.Date;
import java.util.Random;

public class Client {
	boolean quit = false;
	Communicator com;
	Shell sh;
	String command;
	public Client() {
		sh = new Shell(this);
		sh.start();
		command = null;
		com = new Communicator("localhost", 7777, this);
		com.start();
		
	}

	synchronized public boolean connect(String s) {
		if (com.connect(s)) {
			notifyAll();
			return true;
		} else {
			return false;
		}
		
		
	}
	public void quit() {
		sh.connected = false;
		
		quit = true;
		com.interrupt();
		com.quit();
	}
	
	public void userCommand(String s) {
		System.out.println("command: " + s);
		command = s;
	}
	
	public String getCommand() {
		String c = command;
		command = null;
		return c;
	}
	
	synchronized public void waitForConnection() {
		try {
			wait();
		} catch (InterruptedException e) {
			return;
		}
	}
	
	//TODO
	synchronized public void run_test(String name) throws InterruptedException {
		Random r = new Random(new Date().getTime());
		com.connect(name);
		int i = 0;
		while (i < 50) {
			wait(r.nextInt(15)*1000);
			switch (r.nextInt(3)) {
				case 0: command = "listall"; break;
				case 1: command = "msg Yuyu test"; break;
				case 2: command = "show Jack"; break;
				default: break;
			}
			i++;
		}
		
	}
}