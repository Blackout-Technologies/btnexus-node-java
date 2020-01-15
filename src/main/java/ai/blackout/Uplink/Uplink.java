package ai.blackout.Uplink;


import org.json.JSONException;

import ai.blackout.Post.HTTPExceptionWithReason;
import ai.blackout.Post.PostRequest;

import java.io.IOException;

/** Class       : Uplink
 *  Description : This class simply executes a number of PostRequests
 * */
public class Uplink {

    /** Constructor         : Uplink
     *  @param postRequests : Array of PostRequests that will be called in the given order
     **/
    public Uplink(PostRequest... postRequests) throws IOException, HTTPExceptionWithReason, JSONException {
        for (PostRequest postRequest: postRequests) {
            postRequest.send();
        }
    }
}
