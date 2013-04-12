public class Message {
	String message;
	String from, to;
	
	
	public Message(String message, String to, String from) { 
		this.message = message;
		this.to = to;
		this.from = from;
	}
	
	public String toString() {
		return "From " + from + " \""+message + "\"";
	}
}
