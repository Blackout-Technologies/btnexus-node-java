package ai.blackout.Uplink;


import ai.blackout.Post.HTTPExceptionWithReason;
import ai.blackout.Post.PostRequest;
import org.json.simple.parser.ParseException;

import java.io.IOException;

/** Class       : Uplink
 *  Description : This class simply executes a number of PostRequests
 * */
public class Uplink {

    /** Constructor         : Uplink
     *  @param postRequests : Array of PostRequests that will be called in the given order
     **/
    public Uplink(PostRequest... postRequests) throws IOException, ParseException, HTTPExceptionWithReason {
        for (PostRequest postRequest: postRequests) {
            postRequest.send();
        }
    }
}
