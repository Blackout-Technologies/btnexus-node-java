//System imports
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

//3rd party imports
import ai.blackout.node.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


/**
 * This Node shows how to subscribe to different Messages
 */
public class SubUnsub extends Node{

    Thread main;
    boolean shouldRun;
    /**
     * Constructor
     * @throws URISyntaxException
     */
    public SubUnsub(String token, String axonURL, boolean debug) throws URISyntaxException {
        super( token,  axonURL,  debug);
    }

    @Override
    public void setUp(){
        super.setUp();
        Runnable exec = new Runnable() {
            /**
             * Sending current minute and second to the ListeningNode on the printTime and fuseTime callback.
             */
            @Override
            public void run() {
                while (isConnected()) {   // Only run as long as the Node is connected - otherwise there would be 2 Threads after a reconnect!!!TODO:Change like in SendingNOde
                    //ask what to do. sub? unsub?
                    String s = "";
                    String g = "";
                    String t = "";
                    String n = "";
                    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                    try {
                        System.out.print("Subscribe/unsubscribe?[s|u]: ");
                        s = br.readLine();
                        System.out.print("group: ");
                        g = br.readLine();
                        System.out.print("topic: ");
                        t = br.readLine();
                        System.out.print("name: ");
                        n = br.readLine();
                    } catch (IOException e) {
                        onError(e);
                    }
                    switch (s){
                        case "s":
                            try {
                                subscribe(g, t, n, SubUnsub.this::printTime1);
                            } catch (NexusNotConnectedException e) {
                                onError(e);
                            }
                            break;
                        case "u":
                            try {
                                unsubscribe(g, t, n);
                            } catch (Exception e) {
                                onError(e);
                            }
                            break;
                        default:
                            System.out.println("Falsche eingabe es geht nur [s] oder [u]");
                    }
                }
            }
        };
        this.main = new Thread(exec);
        this.shouldRun = true;
    }

    @Override
    public void cleanUp(){
        super.cleanUp();
        try {
            this.shouldRun = false;
            this.main.join();
        } catch (InterruptedException e) {
            onError(e);
        }

    }







    /**
     * Printing the time from the incoming message
     * @param params Parameters from the incoming Message
     * @return This Callback returns null
     */
    public Object printTime1(Object params) {
        JSONArray JSONparams = (JSONArray) params;        // You need to know what you take here JSONObject or JSONArray
        System.out.println("[" + this.nodeName +" print1]: " + JSONparams.get(0) + " || " + JSONparams.get(1));
        return null;
    }

    /**
     * Printing the time from the incoming message
     * @param params Parameters from the incoming Message
     * @return This Callback returns null
     */
    public Object printTime2(Object params) {
        JSONArray JSONparams = (JSONArray) params;        // You need to know what you take here JSONObject or JSONArray
        System.out.println("[" + this.nodeName +" print2]: " + JSONparams.get(0) + " || " + JSONparams.get(1));
        return null;
    }
    /**
     * Fusing the time from the incoming message
     * @param params Parameters from the incoming Message
     * @return the fused time as a String
     */
    public String fuseTime1(Object params) {
        JSONObject JSONparams = (JSONObject) params;        // You need to know what you take here JSONObject or JSONArray
        String fuse = Long.toString((Long) JSONparams.get("min")) + ":" + Long.toString((Long) JSONparams.get("sec")); //Do your calculations
        System.out.println("[" + this.nodeName +" fuse1]: " + fuse);
        return fuse;
    }


    /**
     * Fusing the time from the incoming message
     * @param params Parameters from the incoming Message
     * @return the fused time as a String
     */
    public String fuseTime2(Object params) {
        JSONObject JSONparams = (JSONObject) params;        // You need to know what you take here JSONObject or JSONArray
        String fuse = Long.toString((Long) JSONparams.get("min")) + ":" + Long.toString((Long) JSONparams.get("sec")); //Do your calculations
        System.out.println("[" + this.nodeName +" fuse2]: " + fuse);
        return fuse;
    }

    /**
     * This will be executed after a the Node is successfully connected to the btNexus
     * Here you need to subscribe and set everything else up.
     */
    @Override
    public void onConnected() {
        try {
            //Build tree
            this.subscribe("g1", "t1", "p1", this::printTime1);
            this.subscribe("g2", "t2", "p1", this::printTime1);
            this.subscribe("g2", "t2", "p2", this::printTime1);
            this.subscribe("g2", "t3", "p1", this::printTime1);

            main.start();
        } catch (Exception e) {
            onError(e);
        }
    }
}
