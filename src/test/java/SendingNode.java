//System imports
import java.net.Proxy;
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
    Thread sendThread;
    boolean shouldRun;
    /**
     * Constructor
     * @throws URISyntaxException
     */
    public SendingNode(String token, String axonURL, boolean debug) throws URISyntaxException {
        super( token,  axonURL,  debug);
    }

    /**
     * Constructor
     * @throws URISyntaxException
     */
    public SendingNode(String token, String axonURL, boolean debug, Proxy proxy) throws URISyntaxException {
        super( token,  axonURL,  debug, proxy);
    }

    /**
     * Setting up everything. This will be called everytime before the connection is established
     */
    @Override
    public void setUp(){
        super.setUp();
        Runnable exec = new Runnable() {
            /**
             * Sending current minute and second to the ListeningNode on the printTime and fuseTime callback.
             */
            @Override
            public void run() {
                while(shouldRun){   //  Make sure Thread terminates on reconnect
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
                        onError(e);
                    }
                    try {
                        TimeUnit.SECONDS.sleep(5);
                    } catch (InterruptedException e) {
                        onError(e);
                    }
                }
            }
        };
        this.sendThread = new Thread(exec);
        this.shouldRun = true;
    }

    /**
     * Cleaning up everything. This will be called everytime after a disconnect happend
     * if not differently specified in onDisconnected()
     */
    @Override
    public void cleanUp(){
        super.cleanUp();
        try {
            this.shouldRun = false;
            this.sendThread.join();
        } catch (InterruptedException e) {
            onError(e);
        }

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
     * This will be executed after the Node is successfully connected to the btNexus
     * Here you need to subscribe and start your Threads.
     */
    @Override
    public void onConnected() {
        try {
            this.subscribe("exampleGroup", "example", "fuseTime_response", this::fuseTime_response);
            sendThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
