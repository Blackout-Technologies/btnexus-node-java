import org.json.JSONObject;

import java.net.*;
import java.util.Iterator;
import java.util.List;

import ai.blackout.Post.BTPostRequest;
import ai.blackout.node.Callback;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.WebSocket;

/**
 * ShowCase on how to implement your own Nodes
 * TOKEN=sRdjW9LV2ICqGiblUbSqwRBXN5mxRseiVgwSBpyP6AR;AXON_HOST=dev5.btnexus.ai;PERSONALITYID=7098cff5-f3c7-373d-b361-5730feae614b;INTEGRATIONID=d7463460-fe35-4322-9f13-52a39f264a0f;NEXUS_DEBUG=True;LANG=en-US
 */
public class App
{
    public static void main( String[] args )
    {
        ListeningNode testNode;
        SendingNode testNode2;
        try {
            String token = System.getenv("TOKEN");
            String axon = System.getenv("AXON_HOST");
            System.out.println("APP: AXON: " + axon);
            String persId = System.getenv("PERSONALITYID");
            String lang = System.getenv("LANG");
            String integrationId = System.getenv("INTEGRATIONID"); //IT NEEDS TO A INTEGRATIONID WHICH IS INTEGRATEABLE

            boolean debug = false;
            if(System.getenv("NEXUS_DEBUG") != null){
                debug = true;
            }
            testNode = new ListeningNode(token, axon, debug);
            testNode.connect();
            testNode2 = new SendingNode(token, axon, debug);
            testNode2.connect();
//            JSONObject params = new JSONObject();
//            params.put("personalityId", persId);
//            params.put("language", lang);
//            params.put("integrationId", integrationId);
//            System.out.println( "[PARAMS]: " + params.toJSONString() );
//            System.out.println( "[URL]: " + "https://" + axon + "/api");
//
//
//            BTPostRequest bt = new BTPostRequest("personalityProfile", params , token, "https://" + axon + "/api", new Callback() {
//                @Override
//                public Object apply(Object o) {
//                    JSONObject response = (JSONObject) o;
//                    System.out.println("[RESPONSE]: " + response.toJSONString());
//                    return null;
//                }
//            });
//            bt.send();
//
        }catch(Exception e){
            e.printStackTrace();
        }

    }
}