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
            testNode = new ListeningNode();
            testNode.connect();
            testNode2 = new SendingNode();
            testNode2.connect();
        }catch(Exception e){
            e.printStackTrace();
        }

    }
}