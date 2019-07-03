package ai.blackout.node;

//System imports


//3rd party imports
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


/**
 * This is  a Class to run callbacks in their own Thread and send back the response
 */
public class CallbackExecution implements Runnable {
    private Callback callback;
    private NexusConnector nexus;
    private String topic;
    private String funcName;
    private Object params;
    private String group;

    /**
     * Constructor
     *
     * @param callback The callback which should be executed
     * @param params   The {@link JSONArray} with the parameters for the callbacks
     * @param nexus    An instance of {@link NexusConnector} to send the response
     * @param group    the group of the callback
     * @param topic    the topic of the callback
     * @param funcName The name of the function to construct the response nem
     */
    public CallbackExecution(Callback callback, Object params, NexusConnector nexus, String group, String topic, String funcName) {
        this.callback = callback;
        this.nexus = nexus;
        this.group = group;
        this.topic = topic;
        this.funcName = funcName;
        this.params = params;
    }

    /**
     * The run method will be used by the Thread.
     * Here the callback is applied and the response will be send.
     */
    public void run() {
        Object retVal;
        retVal = this.callback.apply(this.params);

        JSONObject returnParams = new JSONObject();
        returnParams.put("orignCall",this.funcName);
        returnParams.put("originParams",this.params);
        returnParams.put("returnValue",retVal);
        try {
            nexus.publish(this.group, this.topic, this.funcName + "_response", returnParams);
        } catch (NexusNotConnectedException e) {
            System.out.println(e);
        }
    }
}
