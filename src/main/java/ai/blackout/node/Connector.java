package ai.blackout.node;

/**
 * Connector Interface is used to manage what things should be done after the connection to the nexus is established
 */
public interface Connector{
    /**
     * Implement this to handle the things, which should be done after the connection to nexus is established.
     * Put the calls to subscribe() here.
     * And if you want your Node to send an initial Hello message, you should also put it here!
     */
    public void onConnected();
    /**
     * Implement this to handle the things, which should be done after the node is disconnected from the nexus.
     */
    public void onDisconnected(String reason, boolean remote);
    /**
     * Implement this to handle the things, which should be when an error occurs.
     */
    public void onError(Exception ex);
    /**
     * Implement this to handle the things, which should be done before the connection to nexus is established.
     */
    public void setUp();
}

