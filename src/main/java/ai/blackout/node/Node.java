package ai.blackout.node;

//System imports
import java.net.URISyntaxException;

//3rd party imports
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;


/**
 * Node is the class every custom node should inherit from.
 * You need to implement the constructor of your choice and connectCallback() as a minimum
 */
public abstract class Node implements Connector {
    private NexusConnector nexus;
    protected String nodeName;

    /**
     * Constructor for the Node
     */
    public Node() throws URISyntaxException {
        this.nexus = new NexusConnector(this);
        this.nodeName = this.nexus.getParentName();
    }


    /**
     * Method to add a callback to a specific topic and function name.
     *
     * @param group    The group on which the callback should react
     * @param topic    The topic on which the callback should react
     * @param name     The function name within the message the callback should react on
     * @param callback The Callback
     */
    public void subscribe(String group, String topic, String name, Callback callback) throws NexusNotConnectedException {
        nexus.subscribe(group, topic, name, callback);
    }

    /**
     * Method to publish a valid {@link Message} with the intetnt "publish".
     *
     * @param group    The group in which the Message should be published
     * @param topic    The topic in which the Message should be published
     * @param funcName The name of the function which should be triggerd with this message
     * @param params   A {@link JSONArray} which contains the parameters for the function.
     */
    public void publish(String group, String topic, String funcName, JSONArray params) throws NexusNotConnectedException {
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
    public void publish(String group, String topic, String funcName, JSONObject params) throws NexusNotConnectedException {
        nexus.publish(group, topic, funcName, params);
    }

    /**
     * Method to publish a {@link Message} to the nexus on the debug topic.
     *
     * @param debug A debug text message
     */
    public void publishDebug(String debug) throws NexusNotConnectedException {
        nexus.publishDebug(debug);
    }

    /**
     * Method to publish a {@link Message} to the nexus on the warning topic.
     *
     * @param warning A warning text message
     */
    public void publishWarning(String warning) throws NexusNotConnectedException {
        nexus.publishWarning(warning);

    }

    /**
     * Method to publish a {@link Message} to the nexus on the error topic.
     *
     * @param error A error text message
     */
    public void publishError(String error) throws NexusNotConnectedException {
        nexus.publishError(error);

    }


    /**
     * Establish the connection to the nexus.
     */
    public void connect() {
        nexus.connect();
    }
}
