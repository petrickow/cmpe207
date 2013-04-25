import java.util.Scanner;


public class Shell extends Thread  {

	private Server server;
	private Scanner keyboard;
	
	public Shell(Server s) {
		server = s;
	}

	//NO fault handling and no way to shut down!
	public void run() {
		String input;
		keyboard = new Scanner(System.in);
		while (true) {
			input = keyboard.nextLine();
			
			switch (input) {
				case "active": count_active(); break;
				default: System.out.format("not a command\n%s", help()); 
			}
			
		}
	}
	
	private void count_active() {
		System.out.println(server.num_con());
	}
	
	//plan: make methods for altering server parameters and failsafe mechanisms...
	
	private String help() {
		return "log\t-print log\nactive\t-count the number of threads";
	}
}
