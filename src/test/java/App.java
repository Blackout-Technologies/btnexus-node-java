import java.net.*;
import java.util.Iterator;
import java.util.List;

/**
 * ShowCase on how to implement your own Nodes
 *
 */
public class App
{
    public static void main( String[] args )
    {
        System.out.println( "Starting tests!" );
        ListeningNode testNode;
        SendingNode testNode2;
        try {
            String token = System.getenv("TOKEN");
            String axon = System.getenv("AXON_HOST");
            boolean debug = false;
            if(System.getenv("NEXUS_DEBUG") != null){
                debug = true;
            }
            testNode = new ListeningNode(token, axon, debug);
            testNode.connect();
            testNode2 = new SendingNode(token, axon, debug);
            testNode2.connect();
        }catch(Exception e){
            e.printStackTrace();
        }

    }
}