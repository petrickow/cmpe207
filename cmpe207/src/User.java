
public class User {
	private String uname;
	private boolean online;
	
	public User (String uname) {
		this.uname = uname;
		online = false;
	}
	
	public void logon() {
		online = true;
	}
	
	public void logoff() {
		online = false;
	}
	
	public boolean getStatus() {
		return online;
	}
	public String getName() {
		return uname;
	}
}
