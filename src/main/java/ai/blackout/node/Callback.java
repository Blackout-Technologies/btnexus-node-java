package ai.blackout.node;

/**
* Callback Interface is used to have a instance of a Callback to give to the subscribe method of NexusConnector
*/
public interface Callback{
  /**
  * Implement this as you need for your Callback
  */
  public Object apply(Object params);

}
