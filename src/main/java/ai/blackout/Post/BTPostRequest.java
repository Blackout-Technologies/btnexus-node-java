package ai.blackout.Post;


import org.json.simple.JSONObject;

import java.net.MalformedURLException;


import ai.blackout.node.Callback;

/** Class       : BTPostRequest
 *  Description : A class to create BTPostRequests.
 * */

public class BTPostRequest extends PostRequest{

    /************ Class Variables **************/
    private String intent;
    protected JSONObject params;


    /** Constructor     : PostRequest
     *  @param params       : JSONObject holding the parameters for the REST call
     *  @param intent       : The intent is the Endpoint of the REST call
     *  @param accesstoken  : (String) access token for the respective service
     *  @param server       : (String) the desired server to which the request is going to be sent
     *  @param callback     : Callback to be executed upon receiving request
     **/
    public BTPostRequest(String intent, JSONObject params, String accesstoken, String server, Callback callback ) throws MalformedURLException {
        super("blackout-token", accesstoken , server, getBlackOutRequest(intent, params), callback);
        this.intent = intent;

    }

    /** Method          : getBlackOutRequest
     *  Description     : create Blackout protocol compliant request given some content in JSONObject
     *  @param intent   : (String) the intent of the request.
     *  @param params   : (JSONObject) content for the request
     **/
    private static JSONObject getBlackOutRequest(String intent, JSONObject params) {
        JSONObject api          = new JSONObject();
        api.put("version","5.0");
        api.put("intent",intent);
        params.put("api",api);
        return params;
    }
}
