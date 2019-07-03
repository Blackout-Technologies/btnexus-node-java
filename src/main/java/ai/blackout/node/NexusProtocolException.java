package ai.blackout.node;

/** Class       : NexusProtocolException
 *  Description : represents all the errors from the nexus protocol
 * */
public class NexusProtocolException extends Exception {
    private String reason;
    private String stage;
    /**
     * Constructor
     * @param reason    :   The Reason this occured
     * */
    public NexusProtocolException(String stage, String reason){
        super(stage + " failed with reason: " + reason);
        this.reason = reason;
        this.stage = stage;
    }
    public String getReason(){
        return this.reason;
    }
    public String getStage(){
        return this.stage;
    }
}