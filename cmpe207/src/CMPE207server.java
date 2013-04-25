public class CMPE207server {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//TODO - arguments, get max connections, port... any other?
//		default values
		int port = 7777;
		int maxcon = 10;
		
		for (int i = 0; i < args.length; i++) {
			if (args[i].charAt(0) == '-') {
				switch(args[i].charAt(1)) {
					case 'p': port = Integer.parseInt(args[++i]); break; //fault handling if NAN
					case 'c': maxcon = Integer.parseInt(args[++i]); break;
				}
			}
		}
		Server server = new Server(maxcon, port);
		server.runServer();

	}

}
