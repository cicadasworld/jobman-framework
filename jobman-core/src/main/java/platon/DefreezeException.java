package platon;

@SuppressWarnings("serial")
public class DefreezeException extends Exception
{
     public DefreezeException() {
     }

    public DefreezeException(String msg, Exception ex) {
       super(msg, ex);
    }

    public DefreezeException(String msg) {
        super(msg);
    }
}
