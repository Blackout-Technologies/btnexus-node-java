
package ai.blackout.node;

//System imports
import java.util.UUID;

//3rd party imports
import org.json.simple.JSONObject;



/**
 * Messages are basically JSONObjects which follow the blackout protocol
 */
public class Message extends JSONObject{

    private boolean valid = false;
    private String version = "4.2.0";

    /**
     * Construtor for a Message
     *
     * @param intent The intent the Message should contain
     */
    public Message(String intent){
        JSONObject api = new JSONObject();
        api.put("version", this.version);
        Long ts = System.currentTimeMillis();
        Float tsSec = ts.floatValue() / 1000;
        api.put("time",tsSec);
        api.put("intent", intent);
        UUID id = UUID.randomUUID();
        api.put("id", id.toString());

        this.put("api", api);

        this.valid = true;

    }

    /**
     * Construtor for a Message
     *
     * @param json A {@link JSONObject} which contains a Message
     */
    public Message(JSONObject json){
        super(json);
    }

    /**
     * Adds a Authentification to the Message
     *
     * @param authType The type which should be used for the Authentification
     * @param authValue The value for the Authentification
     */
    public void addAuthHeader(String authType, String authValue){

        JSONObject authHeader = new JSONObject();
        authHeader.put("type", authType);
        authHeader.put("value", authValue);
        JSONObject api = (JSONObject) this.get("api");
        api.put("auth", authHeader);
    }

    /**
     * Checks if a Message is valid or not.
     */
    public boolean isValid(){
        return this.valid;
    }
    /**
     * This method validates a Message
     * Considering different protocol versions this method makes sure messages
     * from a older protocol will not be used.
     */
    public void validate() throws Exception{

        JSONObject api = (JSONObject) this.get("api");
        String protocolVersion = (String) api.get("version");
        System.out.println(protocolVersion);
        System.out.println(protocolVersion.charAt(0));
        if(!protocolVersion.equals(this.version) ){
            if (protocolVersion.charAt(0) != this.version.charAt(0)){
                throw new Exception("Major version missmatch!");
            }else{
                throw new Exception("Minor version missmatch.");
            }
        }
        this.valid = true;
    }
}
