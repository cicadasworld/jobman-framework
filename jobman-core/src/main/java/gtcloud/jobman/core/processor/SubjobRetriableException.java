package gtcloud.jobman.core.processor;

public class SubjobRetriableException extends SubjobException {

	private static final long serialVersionUID = -8414956679370070792L;

    public SubjobRetriableException() {
    }

    public SubjobRetriableException(String message) {
        super(message);
    }

    public SubjobRetriableException(Throwable cause) {
        super(cause);
    }

    public SubjobRetriableException(String message, Throwable cause) {
        super(message, cause);
    }

    public SubjobRetriableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }	
}
