package ai.blackout.node;

//System imports
import java.net.Proxy;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

//3rd party imports
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;


/**
 * Node is the class every custom node should inherit from.
 * You need to implement the constructor of your choice and connectCallback() as a minimum
 */
public abstract class Node implements Connector {
    private NexusConnector nexus;
    protected String nodeName;

    /**
     * Constructor for the Node
     * @param token Token for the authentification with the axon
     * @param path URL to axon
     * @param debug should debug messages be sent
     * @param proxy the proxy which should be used for the connection
     */
    public Node(String token, String path, boolean debug, Proxy proxy) throws URISyntaxException {
        Class<?> enclosingClass = this.getClass().getEnclosingClass();
        if (enclosingClass != null) {
            this.nodeName = enclosingClass.getName();
        } else {
            this.nodeName = this.getClass().getName();
        }
        if (!path.substring(path.length() - 1).equals("/")){
            path = path + "/";
        }
        String fullPath = path + this.nodeName;
        this.nexus = new NexusConnector(this, token, fullPath, debug, proxy);
    }

    /**
     * Constructor for the Node
     * @param token Token for the authentification with the axon
     * @param path URL to axon
     * @param debug should debug messages be sent
     */
    public Node(String token, String path, boolean debug) throws URISyntaxException {
        // TODO: 11.12.19 token and path cannot be Null! make meaningfull exception 
 //       System.setProperty("java.net.useSystemProxies", "true");
        Class<?> enclosingClass = this.getClass().getEnclosingClass();
        if (enclosingClass != null) {
            this.nodeName = enclosingClass.getName();
        } else {
            this.nodeName = this.getClass().getName();
        }
        if (!path.substring(path.length() - 1).equals("/")){
            path = path + "/";
        }
//        path = path + this.nodeName; // todo: this is deactivated for now because it seems to be only working without the appended nodeName
        System.out.println("NODE: AXON: " + path);

        this.nexus = new NexusConnector(this, token, path, debug);
    }
    

    /**
     * Method to add a callback to a specific topic and function name.
     *
     * @param group    The group on which the callback should react
     * @param topic    The topic on which the callback should react
     * @param name     The function name within the message the callback should react on
     * @param callback The Callback
     */
    public void subscribe(String group, String topic, String name, Callback callback) throws NexusNotConnectedException, JSONException {
        //Make sure to strip strings
        nexus.subscribe(group.trim(), topic.trim(), name.trim(), callback);
    }

    /**
     * Method to remove a callback from a specific topic and function name.
     * @param group    The group on which the callback should react
     * @param topic    The topic on which the callback should react
     * @param name     The function name within the message the callback should react on
     */
    public void unsubscribe(String group, String topic, String name) throws Exception {
        this.nexus.unsubscribe(group, topic, name);
    }

    /**
     * Method to publish a valid {@link Message} with the intetnt "publish".
     *
     * @param group    The group in which the Message should be published
     * @param topic    The topic in which the Message should be published
     * @param funcName The name of the function which should be triggerd with this message
     * @param params   A {@link JSONArray} which contains the parameters for the function.
     */
    public void publish(String group, String topic, String funcName, JSONArray params) throws NexusNotConnectedException, JSONException {
        nexus.publish(group, topic, funcName, params);
    }

    /**
     * Method to publish a valid {@link Message} with the intetnt "publish".
     *
     * @param group    The group in which the Message should be published
     * @param topic    The topic in which the Message should be published
     * @param funcName The name of the function which should be triggerd with this message
     * @param params   A {@link JSONObject} which contains the parameters for the function.
     */
    public void publish(String group, String topic, String funcName, JSONObject params) throws NexusNotConnectedException, JSONException {
        nexus.publish(group, topic, funcName, params);
    }

    /**
     * Method to publish a {@link Message} to the nexus on the debug topic.
     *
     * @param debug A debug text message
     */
    public void publishDebug(String debug) throws NexusNotConnectedException, JSONException {
        nexus.publishDebug(debug);
    }

    /**
     * Method to publish a {@link Message} to the nexus on the warning topic.
     *
     * @param warning A warning text message
     */
    public void publishWarning(String warning) throws NexusNotConnectedException, JSONException {
        nexus.publishWarning(warning);

    }

    /**
     * Method to publish a {@link Message} to the nexus on the error topic.
     *
     * @param error A error text message
     */
    public void publishError(String error) throws NexusNotConnectedException, JSONException {
        nexus.publishError(error);

    }


    /**
     * Establish the connection to the nexus.
     */
    public void connect() throws URISyntaxException {
        this.setUp();
        nexus.connect();
    }

    /**
     * This will be executed after a the Node is disconnected from the btNexus
     */
    @Override
    public void onDisconnected(String reason, boolean remote) {

        String who;
        if (remote){
            who = "the Axon";
        }else{
            who = "myself";
        }

        System.out.println("[" + this.nodeName + "] Disconnected by " + who + " - " + reason);
    }

    /**
     * Handles errors - if not implemented just publishes the trace to the error topic
     */
    @Override
    public void onError(Exception ex){
        System.out.println("[" + this.nodeName + "]: Error: " + ExceptionHandling.StackTraceToString(ex));
    }
    @Override
    public void setUp(){
        System.out.println("[" + this.nodeName + "] setUp");
    }
}
