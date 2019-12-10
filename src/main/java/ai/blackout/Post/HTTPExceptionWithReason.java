package ai.blackout.Post;

//import javax.xml.ws.http.HTTPException;

/** Class       : HTTPExceptionWithReason
 *  Description : Extends the HTTPException with a Reason
 * */
public class HTTPExceptionWithReason extends Exception {
    private String reason;
    /**
     * Constructor
     *
    // * @param code      :   The HTTP Error Code
     * @param reason    :   The Reason this occured
     * */
    public HTTPExceptionWithReason(String reason){
        super(reason);
        //this.reason = reason;
    }
//    public String getReason(){
//        return this.reason;
//    }
}