package platon;

@SuppressWarnings("serial")
public class FreezeException extends Exception
{
     public FreezeException() {
     }

    public FreezeException(String msg, Exception ex) {
       super(msg, ex);
    }
    
    public FreezeException(String msg) {
        super(msg);
    }
}
