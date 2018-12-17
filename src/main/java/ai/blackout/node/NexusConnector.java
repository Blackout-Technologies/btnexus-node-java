package ai.blackout.node;

//System imports
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.HashMap;

//3rd party imports
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


/**
 * The NexusConnector implements the communication with messages that obey the Blackout protocol.
 */
public class NexusConnector extends WebSocketClient {

    private UUID nodeID;
    private Map<String, Map<String, Map<String, Callback>>> callbacks;
    private boolean isRegistered = false;
    private boolean isConnected = false;
    private String debugTopic = "ai.blackout.debug";
    private String warningTopic = "ai.blackout.warning";
    private String errorTopic = "ai.blackout.error";
    private boolean debug = false;
    private Connector connector;
    private boolean autoReconnect = false;
    private Runnable autoReconnectCallback;
    private String token;
    private String axon;
    private Connector parent;
    private String parentName;

    /**
     * Constructor for the NexusConnector.
     *
     * @param connectCallback the callback which is triggered if connected to the nexus
     */
    public NexusConnector(Connector connectCallback) throws URISyntaxException {
        super(getNexusURI());
        this.token = System.getenv("TOKEN");
        this.axon = System.getenv("AXON_HOST");
        this.debug = false;
        if(System.getenv("NEXUS_DEBUG") != null){
            this.debug = true;
        }
        this.parent = connectCallback;
        Class<?> enclosingClass = this.parent.getClass().getEnclosingClass();
        if (enclosingClass != null) {
            this.parentName = enclosingClass.getName();
        } else {
            this.parentName = this.parent.getClass().getName();
        }

        this.connector = connectCallback;
        this.nodeID = UUID.randomUUID();
        this.callbacks = new HashMap<String, Map<String, Map<String, Callback>>>();
    }

    /**
     * CopyConstructor for the NexusConnector
     * @param nexus the NexusConnector to be copied
     * @throws URISyntaxException
     */
    public NexusConnector(NexusConnector nexus) throws URISyntaxException {
        super(getNexusURI());
        this.token = nexus.getToken();
        this.axon = nexus.getAxon();
        this.debug = nexus.getDebug();
        this.parent = nexus.getParent();
        this.parentName = nexus.getParentName();
        this.connector = nexus.getConnector();
        this.nodeID = nexus.getNodeID();
        this.callbacks = nexus.getCallbacks();
    }


    /**
     * Getter for callbacks
     * @return callbacks
     */
    public Map<String, Map<String, Map<String, Callback>>> getCallbacks() {
        return this.callbacks;
    }
    /**
     * Getter for nodeID
     * @return nodeID
     */
    public UUID getNodeID() {
        return this.nodeID;
    }
    /**
     * Getter for connector
     * @return connector
     */
    public Connector getConnector() {
        return this.connector;
    }
    /**
     * Getter for parentName
     * @return parentName
     */
    public String getParentName() {
        return this.parentName;
    }
    /**
     * Getter for debug
     * @return debug
     */
    public boolean getDebug() {
        return this.debug;
    }
    /**
     * Getter for axon
     * @return axon
     */
    public String getAxon() {
        return this.axon;
    }
    /**
     * Getter for parent
     * @return parent
     */
    public Connector getParent() {
        return this.parent;
    }
    /**
     * Getter for token
     * @return token
     */
    public String getToken() {
        return this.token;
    }

    /**
     * Grabs the ENV variables to construct a URI for the websocket
     * @return URI for the websocket
     * @throws URISyntaxException
     */
    public static URI getNexusURI() throws URISyntaxException {
        String axon = System.getenv("AXON_HOST");
        String concat = "wss://" + axon;
        return new URI(concat);
    }

    /**
     * Join a specific group
     * @param group The group to join to
     * @throws NexusNotConnectedException
     */
    private void join(String group) throws NexusNotConnectedException {
        Message join = new Message("join");
        join.put("groupName", group);
        this.publishMessage(join);
    }
    /**
     * Leave a specific group
     * @param group The group to leave
     * @throws NexusNotConnectedException
     */
    private void leave(String group) throws NexusNotConnectedException {
        Message leave = new Message("leave");
        leave.put("groupName", group);
        this.publishMessage(leave);
    }

    /**
     * Links the Message to the corrosponding Callback
     * @param msg Incoming Message from the btNexus
     */
    private void callbackManager(Message msg) {
        String topic = (String) msg.get("topic");
        topic = topic.replace("ai.blackout.", "");
        JSONObject payload = (JSONObject) msg.get("payload");
        String callbackName = (String) payload.keySet().toArray()[0];
        Object params;
        Callback callback;
        String group = (String) msg.get("group");
        params = payload.get(callbackName);
        callback = this.callbacks.get(group).get(topic).get(callbackName);
        if (callback != null) {
            CallbackExecution exec = new CallbackExecution(callback, params, this, group, topic, callbackName);
            Thread thread = new Thread(exec);
            thread.start();
        } else {
            if (this.debug) {
                try {
                    publishDebug("Callback " + callbackName + " doesn't exist in node " + parentName + " on topic " + topic + " in group " + group);
                } catch (NexusNotConnectedException ne) {
                    System.out.println(ne);
                }
            }
        }
    }

    /**
     * Method to add a callback to a specific topic and function name.
     * @param group    The group on which the callback should react
     * @param topic    The topic on which the callback should react
     * @param name     The function name within the message the callback should react on
     * @param callback A {@link Callback} which has to take a {@link JSONArray} as parameter
     */
    public void subscribe(String group, String topic, String name, Callback callback) throws NexusNotConnectedException {
        if (!this.isConnected) {
            throw new NexusNotConnectedException("You can't subscribe if the connection to the nexus is not established!");
        }

        if (this.callbacks.get(group) == null) {
            this.join(group);
            this.callbacks.put(group, new HashMap<String, Map<String, Callback>>());
        }
        if (this.callbacks.get(group).get(topic) == null) {
            Message sub = new Message("subscribe");
            sub.put("topic", "ai.blackout." + topic);
            this.publishMessage(sub);
            this.callbacks.get(group).put(topic, new HashMap<String, Callback>());
        }
        this.callbacks.get(group).get(topic).put(name, callback);
    }

    /**
     * Here the registering process is triggered.
     * {@inheritDoc}
     */
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        try {
            Message msg = new Message("register");
            msg.put("token", this.token);
            msg.put("host", InetAddress.getLocalHost().getHostName());
            msg.put("ip", "127.0.0.1");
            msg.put("id", this.nodeID.toString());
            JSONObject node = new JSONObject();
            msg.put("node", node);
            send(msg.toString());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        System.out.println("opened connection");
    }

    /**
     * Method to publish a {@link Message} to the nexus.
     * This can be any Message with any intent.
     * It adds the nodeID so that nodes can ommit messages comming from themselves.
     *
     * @param msg A {@link Message} which should be send
     */
    public void publishMessage(Message msg) throws NexusNotConnectedException {
        msg.put("nodeId", this.nodeID.toString());
        try {
            send(msg.toString());
        } catch (WebsocketNotConnectedException e) {
            throw new NexusNotConnectedException("The connection to the nexus broke down!");
        }
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
        try {
            Message msg = new Message("publish");
            msg.put("topic", "ai.blackout." + topic);
            msg.put("host", InetAddress.getLocalHost().getHostName());
            JSONObject payload = new JSONObject();
            payload.put(funcName, params);
            msg.put("payload", payload);
            msg.put("group", group);
            publishMessage(msg);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to publish a valid {@link Message} with the intetnt "publish".
     *
     * @param group    The group in which the Message should be published
     * @param topic    The topic in which the Message shuold be published
     * @param funcName The name of the function which should be triggerd with this message
     * @param params   A {@link JSONObject} which contains the parameters for the function.
     */
    public void publish(String group, String topic, String funcName, JSONObject params) throws NexusNotConnectedException {
        try {
            Message msg = new Message("publish");
            msg.put("topic", "ai.blackout." + topic);
            msg.put("host", InetAddress.getLocalHost().getHostName());
            JSONObject payload = new JSONObject();
            payload.put(funcName, params);
            msg.put("payload", payload);
            msg.put("group", group);
            publishMessage(msg);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }


    /**
     * This Method reacts on incomming Messages.
     * The incoming {@link String} is parsed to a {@link Message}.
     * The corrosponding callback to the Message will be executed.
     * {@inheritDoc}
     */
    @Override
    public void onMessage(String message) {
        Message msg;
        try {
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(message);
            msg = new Message(json);
        } catch (ParseException e) {
            e.printStackTrace();
            System.out.println("received invalid Message!");
            return;
        }
        JSONObject api = (JSONObject) msg.get("api");
        String intent = (String) api.get("intent");
        if (intent.equals("registerSuccess")) {
            this.isRegistered = true;
            this.isConnected = true;
            this.connector.connectCallback();
        } else if (intent.equals("subscribeSuccess")) {
            System.out.println("[Nexus]: Subscribed to: " + msg.get("topic"));
        } else if (intent.equals("subscribeFailed")) {
            System.out.println("[Nexus]: Failed to Subscribed to: " + msg.get("topic"));
        } else if (intent.equals("joinSuccess")) {
            System.out.println("[Nexus]: Joined Group: " + msg.get("groupName"));
        } else if (intent.equals("joinFailed")) {
            System.out.println("[Nexus]: Failed to join Group: " + msg.get("groupName"));
        } else {
            if (this.isRegistered) {
                JSONParser parser = new JSONParser();
                JSONObject json = null;
                try {
                    json = (JSONObject) parser.parse(message);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                callbackManager(new Message(json));
            }
        }
    }

    /**
     * Method to publish a {@link Message} to the nexus on the debug topic.
     *
     * @param debug A debug text message
     */
    public void publishDebug(String debug) throws NexusNotConnectedException {
        if (this.debug) {
            System.out.println(debug);
            Message debMsg = new Message("publish");
            debMsg.put("topic", this.debugTopic);
            JSONObject debugObj = new JSONObject();
            String className = this.getClass().getName();
            debugObj.put("debug", className + ": " + debug);
            debMsg.put("payload", debugObj);
            publishMessage(debMsg);
        }
    }

    /**
     * Method to publish a {@link Message} to the nexus on the warning topic.
     *
     * @param warning A warning text message
     */
    public void publishWarning(String warning) throws NexusNotConnectedException {
        Message warnMsg = new Message("publish");
        warnMsg.put("topic", this.warningTopic);
        JSONObject warnObj = new JSONObject();
        String className = this.getClass().getName();
        warnObj.put("warning", className + ": " + warning);
        warnMsg.put("payload", warnObj);
        publishMessage(warnMsg);
    }

    /**
     * Method to publish a {@link Message} to the nexus on the error topic.
     *
     * @param error A error text message
     */
    public void publishError(String error) throws NexusNotConnectedException {
        Message errorMsg = new Message("publish");
        errorMsg.put("topic", this.errorTopic);
        JSONObject errorObj = new JSONObject();
        String className = this.getClass().getName();
        errorObj.put("error", className + ": " + error);
        errorMsg.put("payload", errorObj);
        publishMessage(errorMsg);
    }

    /**
     * Will be triggered if the connection is closed.
     * {@inheritDoc}
     */
    @Override
    public void onClose(int code, String reason, boolean remote) {
        this.isConnected = false;
        System.out.println("[Nexus]: Connection closed by " + (remote ? "remote peer" : "us") + " Code: " + code + " Reason: " + reason);
        if (this.autoReconnect) {
            this.autoReconnectCallback.run();
        }
    }

    /**
     * Will be triggered if an error occurs.
     * if the error is fatal then onClose will be called additionally
     * {@inheritDoc}
     */
    @Override
    public void onError(Exception ex) {
        System.out.println("[Nexus]: Error:");
        ex.printStackTrace();
    }

    /**
     * Setter for autoReconnect
     * @param autoReconnect switch on/off
     * @param connectionLostTimeout number of seconds after which the connection should be tested
     * @param callback Callback for the case the connection is lost.
     */
    public void setAutoReconnect(boolean autoReconnect, int connectionLostTimeout, Runnable callback) {
        this.autoReconnect = autoReconnect;
        this.autoReconnectCallback = callback;
        setConnectionLostTimeout(connectionLostTimeout);
        startConnectionLostTimer();

    }

    /**
     * Getter for autoReconnect
     */
    public boolean getAutoReconnect() {
        return this.autoReconnect;
    }
}
