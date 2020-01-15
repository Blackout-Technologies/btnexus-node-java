package ai.blackout.node;

//System imports

import java.net.*;
import java.util.*;
import java.util.regex.Pattern;

//3rd party imports
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.WebSocket;


/**
 * The NexusConnector implements the communication with messages that obey the Blackout protocol.
 */
public class NexusConnector {

    private String protocolVersion = "5.0";
    private String nodeID;
    private Map<String, Map<String, Map<String, Callback>>> callbacks;
    private boolean isRegistered = false;
    private boolean isConnected = false;
    private String debugTopic = "ai.blackout.debug";
    private String warningTopic = "ai.blackout.warning";
    private String errorTopic = "ai.blackout.error";
    private boolean debug = false;
    private String token;
    private String axon;
    private Connector parent;
    private String parentName;
    private Socket socket;
    private boolean remote;
    private String reason;
    private boolean reconnect;

    /**
     * Constructor for the NexusConnector.
     *
     * @param connectCallback the callback which is triggered if connected to the nexus
     * @param token           Token for the authentification with the axon
     * @param axonURL         URL to axon
     * @param debug           should debug messages be sent
     */
    public NexusConnector(Connector connectCallback, String token, String axonURL, boolean debug) {
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
        this.callbacks = new HashMap<String, Map<String, Map<String, Callback>>>();



    }

    /**
     * Getter for isConnected
     *
     * @return isConnected
     */
    public boolean isConnected() {
        return this.isConnected;
    }

    /**
     * Getter for callbacks
     *
     * @return callbacks
     */
    public Map<String, Map<String, Map<String, Callback>>> getCallbacks() {
        return this.callbacks;
    }

    /**
     * Getter for nodeID
     *
     * @return nodeID
     */
    public String getNodeID() {
        return this.nodeID;
    }

    /**
     * Getter for connector
     *
     * @return connector
     */
    public Connector getConnector() {
        return this.parent;
    }

    /**
     * Getter for parentName
     *
     * @return parentName
     */
    public String getParentName() {
        return this.parentName;
    }

    /**
     * Getter for debug
     *
     * @return debug
     */
    public boolean getDebug() {
        return this.debug;
    }

    /**
     * Getter for axon
     *
     * @return axon
     */
    public String getAxon() {
        return this.axon;
    }

    /**
     * Getter for parent
     *
     * @return parent
     */
    public Connector getParent() {
        return this.parent;
    }

    /**
     * Getter for token
     *
     * @return token
     */
    public String getToken() {
        return this.token;
    }

    /**
     * Join a specific group
     *
     * @param group The group to join to
     * @throws NexusNotConnectedException
     */
    private void join(String group) throws NexusNotConnectedException, JSONException {
        Message join = new Message("join");
        join.put("groupName", group);
        this.publishMessage(join);
    }

    /**
     * Leave a specific group
     *
     * @param group The group to leave
     * @throws NexusNotConnectedException
     */
    private void leave(String group) throws NexusNotConnectedException, JSONException {
        Message leave = new Message("leave");
        leave.put("groupName", group);
        this.publishMessage(leave);
    }

    /**
     * Links the Message to the corrosponding Callback
     *
     * @param msg Incoming Message from the btNexus
     */
    private void callbackManager(Message msg) throws JSONException {
        String topic = (String) msg.get("topic");
        topic = topic.replace("ai.blackout.", "");
        JSONObject payload = (JSONObject) msg.get("payload");
        String callbackName = (String) payload.keys().next(); //Can only have one callbackName by definition of the protocol
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
     *
     * @param group    The group on which the callback should react
     * @param topic    The topic on which the callback should react
     * @param name     The function name within the message the callback should react on
     * @param callback A {@link Callback} which has to take a {@link JSONArray} as parameter
     */
    public void subscribe(String group, String topic, String name, Callback callback) throws NexusNotConnectedException, JSONException {
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
     *
     * @param group The group on which the callback should react
     * @param topic The topic on which the callback should react
     * @param name  The function name within the message the callback should react on
     */
    public void unsubscribe(String group, String topic, String name) throws NexusProtocolException, JSONException, NexusNotConnectedException {
        //check for existence
        Callback callback;
        try {
            callback = this.callbacks.get(group).get(topic).get(name);
        } catch (NullPointerException ex) {
            callback = null;
        }
        if (callback == null) {
            throw new NexusProtocolException("Unsubscribe from " + name, "non existing Callback! Choose one out of: " + this.callbacks.toString());
        }
        // if the only callback on topic -> remove topic from map
        if (this.callbacks.get(group).get(topic).size() == 1) {
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
        } else {
            this.callbacks.get(group).get(topic).remove(name);
        }

    }

    /**
     * Method to publish a {@link Message} to the nexus.
     * This can be any Message with any intent.
     * It adds the nodeID so that nodes can ommit messages comming from themselves.
     *
     * @param msg A {@link Message} which should be send
     */
    public void publishMessage(Message msg) throws NexusNotConnectedException, JSONException {
        if (this.isRegistered && this.isConnected){
            msg.put("nodeId", this.nodeID);
            this.socket.send(msg.toString());
        } else {
            throw new NexusNotConnectedException("Couldn't send " + msg.toString() + " - You are not connected to the Axon");
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
    public void publish(String group, String topic, String funcName, JSONArray params) throws NexusNotConnectedException, JSONException {
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
    public void publish(String group, String topic, String funcName, JSONObject params) throws NexusNotConnectedException, JSONException {
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
    public void onMessage(String message) throws JSONException, NexusNotConnectedException {
        JSONObject json = new JSONObject(message);
        Message msg = new Message(json);
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
            this.isRegistered = true;
            this.isConnected = true;

            if(major > msgMajor) {
                this.disconnect("The Axon you are trying to connect to is no longer supported! Major Version must be greater than " + major + " but is " + msgMajor, false);
                return;
            }
            if (major == msgMajor && minor > msgMinor){
                publishDebug("Minor Version missmatch");
            }
                this.publishDebug("Registered successfully");

            this.parent.onConnected();
        } else if (intent.equals("registerFailed")) {
            onError(new NexusProtocolException("Register", msg.get("reason").toString()));
            //System.out.println("[" + this.parentName + "]: Register failed with reason: " + msg.get("reason"));
        } else if (intent.equals("subscribeSuccess")) {
                this.publishDebug("Subscribed to: " + msg.get("topic"));
        } else if (intent.equals("subscribeFailed")) {
            this.onError(new NexusProtocolException("Subscribe to " +  msg.get("topic").toString(), msg.get("reason").toString()));
        } else if (intent.equals("joinSuccess")) {
                this.publishDebug("Joined Group: " + msg.get("groupName"));
        } else if (intent.equals("joinFailed")) {
            this.onError(new NexusProtocolException("Join " + msg.get("groupName"), msg.get("reason").toString()));
        } else if (intent.equals("leaveSuccess")) {
                this.publishDebug("Left Group: " + msg.get("groupName"));
        } else if (intent.equals("leaveFailed")) {
            this.onError(new NexusProtocolException("Leave Group " + msg.get("groupName"), msg.get("reason").toString()));
        } else if (intent.equals("unsubscribeSuccess")) {
                this.publishDebug("Unsubscribed from topic: " + msg.get("topic"));
        } else if (intent.equals("unsubscribeFailed")) {
            onError(new NexusProtocolException("Unsubscribe from " + msg.get("topic"), msg.get("reason").toString()));
        } else {
            if (this.isRegistered) {
                callbackManager(msg);
            }
        }
    }

    /**
     * Method to publish a {@link Message} to the nexus on the debug topic.
     *
     * @param debug A debug text message
     */
    public void publishDebug(String debug) throws NexusNotConnectedException, JSONException {
        if (this.debug) {
            Message debMsg = new Message("publish");
            debMsg.put("topic", this.debugTopic);
            JSONObject debugObj = new JSONObject();
            String className = this.parentName;
            System.out.println("[" + className + "]: Debug: " + debug);
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
    public void publishWarning(String warning) throws NexusNotConnectedException, JSONException {
        Message warnMsg = new Message("publish");
        warnMsg.put("topic", this.warningTopic);
        JSONObject warnObj = new JSONObject();
        String className = this.parentName;
        System.out.println("[" + className + "]: Warning: " + warning);
        warnObj.put("warning", className + ": " + warning);
        warnMsg.put("payload", warnObj);
        publishMessage(warnMsg);
    }

    /**
     * Method to publish a {@link Message} to the nexus on the error topic.
     *
     * @param error A error text message
     */
    public void publishError(String error) throws NexusNotConnectedException, JSONException {
        Message errorMsg = new Message("publish");
        errorMsg.put("topic", this.errorTopic);
        JSONObject errorObj = new JSONObject();
        String className = this.parentName;
        System.out.println("[" + className + "]: Error: " + error);
        errorObj.put("error", className + ": " + error);
        errorMsg.put("payload", errorObj);
        publishMessage(errorMsg);
    }

    public void connect(IO.Options opts) throws URISyntaxException {
        this.setUp();
        if (opts.transports == null) {
            opts.transports = new String[]{WebSocket.NAME};
        }
        this.reconnect = opts.reconnection;
        this.socket = IO.socket("https://" + this.axon, opts);
        this.socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                NexusConnector.this.isConnected = true;
            }

        }).on("btnexus-registration", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                for (Object obj : args) { // per definition of the protocol this is only one...
                    JSONObject data = (JSONObject) obj;
                    try {
                        NexusConnector.this.nodeID = (String) data.get("nodeId");
                        Message msg = new Message("register");
                        msg.put("token", NexusConnector.this.token);
                        msg.put("host", InetAddress.getLocalHost().getHostName());
                        msg.put("ip", "127.0.0.1");
                        msg.put("id", NexusConnector.this.nodeID);
                        JSONObject node = new JSONObject(); // This is empty in this implementation
                        msg.put("node", node);
                        NexusConnector.this.socket.send(msg.toString());
                    } catch (JSONException | UnknownHostException e) {
                        NexusConnector.this.onError(e);
                    }
                }


            }

        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                // TODO: can more info about the reason be extracted here?
//                for (Object obj : args) { // per definition of the protocol this is only one...
////                    JSONObject data = (JSONObject) obj;
//                    System.out.println("disconnect: " + obj);
//                }
                NexusConnector.this.onDisconnected(NexusConnector.this.reason, NexusConnector.this.remote);
            }

        }).on(Socket.EVENT_MESSAGE, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                for (Object obj : args) { // per definition of the protocol this is only one...
                    try {
                        NexusConnector.this.onMessage((String)obj);
                    } catch (NexusNotConnectedException | JSONException e) {
                        NexusConnector.this.onError(e);
                    }
                }
            }

        });

        this.socket.connect();
    }

    public void connect() throws URISyntaxException {
        connect(new IO.Options());
    }

    public void disconnect(String reason, boolean remote){
        this.remote = remote;
        this.reason = reason;
    }
    /**
     * This will be executed after a the Node is disconnected from the btNexus
     *
     * @param reason the reason for the disconnect
     * @param remote shows if the closing was triggered remotely
     */
    public void onDisconnected(String reason, boolean remote){
        this.callbacks = new HashMap<String, Map<String, Map<String, Callback>>>();
        this.isConnected = false;
        this.isRegistered = false;
        this.parent.onDisconnected(reason, remote);
        if (this.reconnect){
            this.parent.setUp();
        }
    }


    public void setUp(){
        this.remote = true;
        this.reason = "Axon closed connection";
        this.parent.setUp();
    }

    /**
     * Will be triggered if an error occurs.
     * if the error is fatal then onClose will be called additionally
     * {@inheritDoc}
     */
    public void onError(Exception ex) {
        try {
            throw (ex);
        }catch (NexusProtocolException e){
            try {
                this.publishDebug(e.getStage() + ": " + e.getReason());
            } catch (NexusNotConnectedException | JSONException exc) {
                this.onError(exc);
            }
        }catch (NexusNotConnectedException e){
//            System.out.println("[" + this.parentName + "]: Error: " + ExceptionHandling.StackTraceToString(ex));
        }catch (Exception e){
            try {
                this.publishError(ExceptionHandling.StackTraceToString(ex));
            } catch (NexusNotConnectedException | JSONException exc) {
                this.onError(exc);
            }
        }
        this.parent.onError(ex);
    }
}
