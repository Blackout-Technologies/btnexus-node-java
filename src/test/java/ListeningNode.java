//System imports
import java.net.URISyntaxException;

//3rd party imports
import ai.blackout.node.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


/**
 * This Node shows how to subscribe to different Messages
 */
public class ListeningNode extends Node{

    /**
     * Constructor
     * @throws URISyntaxException
     */
    public ListeningNode() throws URISyntaxException {
        super();
    }

    /**
     * Printing the time from the incoming message
     * @param params Parameters from the incoming Message
     * @return This Callback returns null
     */
    public Object printTime(Object params) {
        JSONArray JSONparams = (JSONArray) params;        // You need to know what you take here JSONObject or JSONArray
        System.out.println("[" + this.nodeName +"]: " + JSONparams.get(0) + " || " + JSONparams.get(1));
        return null;
    }
    /**
     * Fusing the time from the incoming message
     * @param params Parameters from the incoming Message
     * @return the fused time as a String
     */
    public String fuseTime(Object params) {
        JSONObject JSONparams = (JSONObject) params;        // You need to know what you take here JSONObject or JSONArray
        String fuse = Long.toString((Long) JSONparams.get("min")) + ":" + Long.toString((Long) JSONparams.get("sec")); //Do your calculations
        return fuse;
    }
    /**
     * This will be executed after a the Node is succesfully connected to the btNexus
     * Here you need to subscribe and set everything else up.
     */
    @Override
    public void connectCallback() {
        try {
            this.subscribe("exampleGroup", "example", "printTime", this::printTime);
            this.subscribe("exampleGroup", "example", "fuseTime", this::fuseTime);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
