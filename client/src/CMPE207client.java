/*This class will be used for testin purposes
 *
 */


public class CMPE207client {
	static Client clients[];
	static Client client;
	public static void main(String[] args) {
		
 
		
		
		if (args.length == 1) 
			test();
		else 
			client = new Client();
			
			
	}
	private static void test() {
		clients = new Client[3];
		
		String names[] = new String[3];
		
		names[0] = "Jack";
		names[1] = "Cato";
		names[2] = "Yuyu";
				
				
		for (int i = 0; i < 3; i++) {
			clients[i] = new Client();
			try {
				clients[i].run_test(names[i]);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}