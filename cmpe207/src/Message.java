public class Message {
	String message;
	String from, to;
	int msgid;
	
	public Message(int msgid, String message, String to, String from) { 
		this.message = message;
		this.to = to;
		this.from = from;
		this.msgid = msgid;
	}
	
	public String toString() {
		return "From " + from + " \""+message + "\"";
	}
}
