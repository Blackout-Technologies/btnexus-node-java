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
 * TOKEN=JbUc0AAsw2ml29KWpJQzFeNT0J7CLgFGoQSdDSe4TBm;AXON_HOST=dev5.btnexus.ai;PERSONALITYID=7098cff5-f3c7-373d-b361-5730feae614b;INTEGRATIONID=d7463460-fe35-4322-9f13-52a39f264a0f;NEXUS_DEBUG=True;LANG=en-US
 */
public class App
{
    public static void main( String[] args )
    {
//        System.out.println( "Starting tests!" );
//        Socket socket;
//        IO.Options opts = new IO.Options();
//        opts.transports = new String[]{ WebSocket.NAME};
//        try {
//            String url =  "https://dev5.btnexus.ai/T"; // TODO: 13.01.20 here is the problem: It shouldnt work without slash but it only works without slash! :(
//            System.out.println("USING: " + url);
//            socket = IO.socket(url, opts);
//            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
//
//                @Override
//                public void call(Object... args) {
//                    System.out.println("Connected!");
//                }
//
//            }).on("btnexus-registration", new Emitter.Listener() {
//
//                @Override
//                public void call(Object... args) {
//                    JSONObject obj = (JSONObject)args[0];
//                    System.out.println("btnexus-registration: " + obj);
//                }
//
//            }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
//
//                @Override
//                public void call(Object... args) {
//                    System.out.println("Disconnected!");
//                }
//
//            });
//
//
//            socket.connect();
//        }catch (URISyntaxException e) {
//            System.out.println("SocketIO connect: " + e);
//        }






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