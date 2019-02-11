package ai.blackout.node;

//System imports
import java.net.*;
import java.util.*;
import java.util.regex.Pattern;

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

    private String protocolVersion = "4.2";
    private UUID nodeID;
    private Map<String, Map<String, Map<String, Callback>>> callbacks;
    private boolean isRegistered = false;
    private boolean isConnected = false;
    private String debugTopic = "ai.blackout.debug";
    private String warningTopic = "ai.blackout.warning";
    private String errorTopic = "ai.blackout.error";
    private boolean debug = false;
    private Connector connector;
    private String token;
    private String axon;
    private Connector parent;
    private String parentName;
    private Proxy proxy;

    /**
     * Constructor for the NexusConnector.
     *
     * @param connectCallback the callback which is triggered if connected to the nexus
     * @param token Token for the authentification with the axon
     * @param axonURL URL to axon
     * @param debug should debug messages be sent
     */
    public NexusConnector(Connector connectCallback, String token, String axonURL, boolean debug) throws URISyntaxException {
        super(getNexusURI(axonURL));
        this.token = token;
        this.axon = axonURL;
        this.debug = debug;
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
        //GET SYSTEMPROXY
        try {
            List<Proxy> l = ProxySelector.getDefault().select(getNexusURI(this.axon));
            this.proxy = l.get(0);
            this.setProxy(this.proxy);
        }
        catch (URISyntaxException e) {
            this.onError(e);
        }

    }

    /**
     * Constructor for the NexusConnector.
     *
     * @param connectCallback the callback which is triggered if connected to the nexus
     * @param token Token for the authentification with the axon
     * @param axonURL URL to axon
     * @param debug should debug messages be sent
     * @param proxy the proxy which should be used for the connection
     */
    public NexusConnector(Connector connectCallback, String token, String axonURL, boolean debug, Proxy proxy) throws URISyntaxException {
        this( connectCallback,  token,  axonURL,  debug);
        this.proxy = proxy;
        this.setProxy(this.proxy);
    }

    /**
     * CopyConstructor for the NexusConnector
     * @param nexus the NexusConnector to be copied
     * @throws URISyntaxException
     */
    public NexusConnector(NexusConnector nexus) throws URISyntaxException {
        super(getNexusURI(nexus.getAxon()));
        this.token = nexus.getToken();
        this.axon = nexus.getAxon();
        this.debug = nexus.getDebug();
        this.parent = nexus.getParent();
        this.parentName = nexus.getParentName();
        this.connector = nexus.getConnector();
        this.nodeID = nexus.getNodeID();
        this.callbacks = new HashMap<String, Map<String, Map<String, Callback>>>();// callbacks cannot be copied because they are not subscribed - a copied nexusConnector is not connected yet.
        this.proxy = nexus.getProxy();
        this.setProxy(this.proxy);
    }
    public boolean isConnected() {
        return this.isConnected;
    }

    /**
     * Getter for proxy
     * @return proxy
     */
    private Proxy getProxy() {
        return this.proxy;
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
    public static URI getNexusURI(String axonURL) throws URISyntaxException {
        String concat = "wss://" + axonURL;
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
                    onError(ne);
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
     * Method to remove a callback from a specific topic and function name.
     * @param group    The group on which the callback should react
     * @param topic    The topic on which the callback should react
     * @param name     The function name within the message the callback should react on
     */
    public void unsubscribe(String group, String topic, String name) throws Exception {
        //check if connected
        if (!this.isConnected) {
            throw new NexusNotConnectedException("You can't unsubscribe if the connection to the nexus is not established!");
        }
        //check for existence
        Callback callback;
        try{
            callback = this.callbacks.get(group).get(topic).get(name);
        }catch (NullPointerException ex){
            callback = null;
        }
        if(callback == null){
            throw new Exception("Can't unsubscribe from non existing Callback!\nChoose one out of: " + this.callbacks.toString());
        }
        // if the only callback on topic -> remove topic from map
        if (this.callbacks.get(group).get(topic).size() == 1){
            this.callbacks.get(group).remove(topic);
            Message unsub = new Message("unsubscribe");
            unsub.put("topic", topic);
            this.publishMessage(unsub);


            // if it was the only topic on group -> remove group from map
            if (this.callbacks.get(group).size() == 0) {
                this.callbacks.remove(group);
                //leave message
                leave(group);
            }
        }else{
            this.callbacks.get(group).get(topic).remove(name);
        }

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
            System.out.println("[" + this.parentName + "]: opened connection with " + this.proxy);

        } catch (UnknownHostException e) {
            this.onError(e);
        }

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
            this.onError(e);
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
            this.onError(e);
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
            this.onError(e);
            return;
        }
        JSONObject api = (JSONObject) msg.get("api");
        String intent = (String) api.get("intent");
        if (intent.equals("registerSuccess")) {
            //Check Version here
            String [] parts = this.protocolVersion.split(Pattern.quote("."));
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            String msgVersion = (String)api.get("version");
            parts = msgVersion.split(Pattern.quote("."));
            int msgMajor = Integer.parseInt(parts[0]);
            int msgMinor = Integer.parseInt(parts[1]);

            if(major > msgMajor) {
                onClose(-1, "The Axon you are trying to connect to is no longer supported! Major Version must be greater than " + major + " but is " + msgMajor, false);
                return;
            }
            if (major == msgMajor && minor > msgMinor){
                    //info minor is smaller
                try {
                    publishDebug("Minor Version missmatch");
                }catch (NexusNotConnectedException ne){
                    onError(ne);
                }
            }



            System.out.println("[" + this.parentName + "]: Registered successfully");
            this.isRegistered = true;
            this.isConnected = true;
            this.connector.onConnected();
        } else if (intent.equals("registerFailed")) {
            System.out.println("[" + this.parentName + "]: Register failed with reason: " + msg.get("reason"));
        } else if (intent.equals("subscribeSuccess")) {
            System.out.println("[" + this.parentName + "]: Subscribed to: " + msg.get("topic"));
        } else if (intent.equals("subscribeFailed")) {
            System.out.println("[" + this.parentName + "]: Failed to Subscribed to: " + msg.get("topic"));
        } else if (intent.equals("joinSuccess")) {
            System.out.println("[" + this.parentName + "]: Joined Group: " + msg.get("groupName"));
        } else if (intent.equals("joinFailed")) {
            System.out.println("[" + this.parentName + "]: Failed to join Group: " + msg.get("groupName"));
        } else if (intent.equals("leaveSuccess")) {
            System.out.println("[" + this.parentName + "]: left Group: " + msg.get("groupName"));
        } else if (intent.equals("leaveFailed")) {
            System.out.println("[" + this.parentName + "]: Failed to leave Group: " + msg.get("groupName"));
        } else if (intent.equals("unsubscribeSuccess")) {
            System.out.println("[" + this.parentName + "]: unsubscribed from topic: " + msg.get("topic"));
        } else if (intent.equals("unsubscribeFailed")) {
            System.out.println("[" + this.parentName + "]: Failed to unsubscribed from topic: " + msg.get("topic"));
        } else {
            if (this.isRegistered) {
//                JSONParser parser = new JSONParser();
//                JSONObject json = null;
//                try {
//                    json = (JSONObject) parser.parse(message);
//                } catch (ParseException e) {
//                    this.onError(e);
//                }
//                callbackManager(new Message(json));
                //System.out.println(message);
                callbackManager(msg);
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
            System.out.println("[" + this.parentName + "]: Debug: " +debug);
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
        System.out.println("[" + this.parentName + "]: Connection closed by " + (remote ? "remote peer" : "us") + " Code: " + code + " Reason: " + reason);
        this.connector.onDisconnected();
        //if (this.autoReconnect) {
        //    this.autoReconnectCallback.run();
        //}
    }

    /**
     * Will be triggered if an error occurs.
     * if the error is fatal then onClose will be called additionally
     * {@inheritDoc}
     */
    @Override
    public void onError(Exception ex) {
        String trace = ExceptionHandling.StackTraceToString(ex);
        System.out.println("[" + this.parentName + "]: Error: " + trace);
        try {
            publishError("[" + this.parentName + "]: Error: " + trace);
        } catch (NexusNotConnectedException e) {
            e.printStackTrace();
        }

    }

}
