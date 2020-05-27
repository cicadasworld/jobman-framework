package gtcloud.jobman.core.processor;

public class SubjobException extends Exception {

    private static final long serialVersionUID = 8901239852453266568L;

    public SubjobException() {
    }

    public SubjobException(String message) {
        super(message);
    }

    public SubjobException(Throwable cause) {
        super(cause);
    }

    public SubjobException(String message, Throwable cause) {
        super(message, cause);
    }

    public SubjobException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
