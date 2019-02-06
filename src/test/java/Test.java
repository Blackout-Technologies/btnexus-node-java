import java.net.*;
import java.util.Iterator;
import java.util.List;

/**
 * ShowCase on how to implement your own Nodes
 *
 */
public class Test
{
    public static void main( String[] args )
    {
        System.out.println( "Starting tests!" );
        SubUnsub subUnsub;
        try {
            String token = System.getenv("TOKEN");
            String axon = System.getenv("AXON_HOST");
            boolean debug = false;
            if(System.getenv("NEXUS_DEBUG") != null){
                debug = true;
            }
            subUnsub = new SubUnsub(token, axon, true);
            subUnsub.connect();
        }catch(Exception e){
            e.printStackTrace();
        }

    }
}