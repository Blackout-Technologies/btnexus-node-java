package ai.blackout.node;

//System imports
import java.io.IOException;

/**
 * Exception which indicates the nexus is not yet connected
 */
public class NexusNotConnectedException extends IOException {
  /**
   * Constructor overloads constructor of IOException
   * @param message A message indicating what went wrong
   */
  public NexusNotConnectedException(String message){
    super(message);
  }
}
