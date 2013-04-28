public class CMPE207client {

	public CMPE207client() {
	}

	public static void main(String[] args) {

		Client client = new Client("localhost", 7777);
		client.runClient();	
	}
}