package ai.blackout.Uplink;


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
    public Uplink(PostRequest... postRequests) throws IOException, ParseException {
        for (PostRequest postRequest: postRequests) {
            postRequest.send();
        }
    }
//    /************ Class Variables **************/
//    private final String deviceID;
//    private final String accessToken;
//    private final String instanceURL;
//
//    private org.json.simple.JSONObject reqParamsPersonalityID;
//    private org.json.simple.JSONObject reqParamsAxonURL;
//
//    public JSONObject configs;
//
//    private final Callback IDCallback;
//    private final Callback mainCallback;
//    /*****************Methods******************/
//
//    public Uplink(String accessToken, String instanceURL, String deviceID,Callback callback)
//    {
//        this.accessToken                = accessToken;
//        this.instanceURL                = instanceURL;
//        this.deviceID                   = deviceID;
//        this.reqParamsPersonalityID     = new JSONObject();
//        this.reqParamsAxonURL           = new JSONObject();
//        this.reqParamsPersonalityID     .put("robotId",this.deviceID);
//        this.reqParamsAxonURL           .put("robotId",this.deviceID);
//        this.mainCallback               = callback;
//        this.configs                    = new JSONObject();
//        this.IDCallback                 = new Callback() {
//            @Override
//            public Object apply(Object params) {
//                JSONObject response = (JSONObject)params;
//                Uplink.this.configs.put("personalityId",Uplink.this.extractPersonalityID(response));
//              //  Log.e("FIRST REQUEST CONTENT",response.toString());
//                Uplink.this.mainCallback.apply(Uplink.this.configs);
//                return true;
//            }
//
//
//        };
//
//        this.getPersonalityID();
//    }
//
//    public void getPersonalityID()
//    {
//        PostRequest request = new PostRequest("deviceConfig",this.accessToken,this.instanceURL,this.IDCallback);
//        request.blackOutRequest(this.reqParamsPersonalityID);
//
//        Runnable run = new Runnable() {
//            @Override
//            public void run() {
//                Log.e("Sending Request","With content"+request.request.toString());
//                request.send("");
//            }
//        };
//        Thread t = new Thread(run);
//        t.start();
//    }
//    private String extractPersonalityID(JSONObject response)
//    {
//        try{
//            JSONObject device = (JSONObject)response.get("device");
//            return device.get("personalityId").toString();
//        }catch(NullPointerException e)
//        {
//            Log.e("Extract Personality","error with robot Object",e);
//            return "";
//        }
//
//    }
}
