package gtcloud.common.basetypes;

public class StatusCodeException extends Exception {
	private static final long serialVersionUID = -4354570411112040301L;

	private int code;
	
    public StatusCodeException(int code) {
        super();
        this.code = code;
    }

    public StatusCodeException(int code, String message) {
        super(message);
        this.code = code;        
    }

    public StatusCodeException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;;        
    }

    public StatusCodeException(int code, Throwable cause) {
        super(cause);
        this.code = code;        
    }

	public int getCode() {
		return code;
	}
	
}
