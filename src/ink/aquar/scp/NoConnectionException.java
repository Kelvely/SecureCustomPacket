package ink.aquar.scp;

public class NoConnectionException extends RuntimeException {
	
	private static final long serialVersionUID = 1499031180680426223L;
	
	public NoConnectionException() {
		super();
	}
	
	public NoConnectionException(String message) {
		super(message);
	}

}
