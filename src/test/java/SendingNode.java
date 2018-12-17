//System imports
import java.net.URISyntaxException;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

//3rd party imports
import ai.blackout.node.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


/**
 * This Node shows how to implement an active Node which sends different Messages
 */
public class SendingNode extends Node{

    /**
     * Constructor
     * @throws URISyntaxException
     */
    public SendingNode() throws URISyntaxException {
        super();
    }

    /**
     * Reacting to the fused Time with a print in a specific shape.
     *
     * @param params Parameters from the incoming Message
     * @return This Callback returns null
     */
    public String fuseTime_response(Object params) {
        JSONObject JSONparams = (JSONObject) params;        // You need to know what you take here JSONObject or JSONArray
        String fusedTime = (String) JSONparams.get("returnValue");
        System.out.println("[" + this.nodeName + "]: " + fusedTime);
        return null;
    }

    /**
     * This will be executed after a the Node is succesfully connected to the btNexus
     * Here you need to subscribe and set everything else up.
     */
    @Override
    public void connectCallback() {
        try {
            this.subscribe("exampleGroup", "example", "fuseTime_response", this::fuseTime_response);

            Runnable exec = new Runnable() {
                /**
                 * Sending currenct minute and second to the ListeningNode on the printTime and fuseTime callback.
                 */
                @Override
                public void run() {
                    while(true){
                        LocalTime time = java.time.LocalTime.now();
                        int min = time.getMinute();
                        int sec = time.getSecond();
                        JSONArray arrayParams = new JSONArray();
                        arrayParams.add(min);
                        arrayParams.add(sec);
                        JSONObject objectParams = new JSONObject();
                        objectParams.put("min", min);
                        objectParams.put("sec", sec);
                        try {
                            publish("exampleGroup", "example", "printTime", arrayParams);
                            publish("exampleGroup", "example", "fuseTime", objectParams);
                        } catch (NexusNotConnectedException e) {
                            e.printStackTrace();
                        }
                        try {
                            TimeUnit.SECONDS.sleep(5);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            Thread thread = new Thread(exec);
            thread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
