# Blackout Nexus Node for Java

![Blackout logo](https://www.blackout.ai/wp-content/uploads/2018/08/logo.png)

|||
|---|---|
|Author|Adrian Lubitz|
|Author|Marc Fiedler|
|Email|dev@blackout.ai|
|Latest stable version|3.1.0|
|Required Axon versions| >= 3.0.0|
|Runs on|Java|
|State|`Stable`|

# Introduction

The `nexus` by Blackout Technologies is a platform to create Digital Assistants and to connect them via the internet to multiple platforms. Those platforms can be websites, apps or even robots. The `nexus` consists of two major parts, first being the `btNexus` and second the nexusUi. The `btNexus` is the network that connects the A.I. with the nexusUi and the chosen interfaces. The nexusUi is the user interface, that allows the user to create their own A.I.-based Digital Assistant. Those Digital Assistants can be anything, support chatbots or even robot personalities.   
Every user has one or multiple nexusUi instances or short nexus instances, which means, it's their workspace. One nexusUi / nexus instance can host multiple personalities.

Every part of the `btNexus` is a Node. These Nodes can react on messages and send messages through the `btNexus`. To understand how Nodes work the following key concepts need to be clear.

## Nodes
Nodes are essentially little programs. It is not important in which language these programs are implemented.
More important is that they share `Messages` between them in certain `Groups` and `Topics`.
So every node has its very unique purpose. It reacts on `Messages` with a `Callback` which is subscribed to a `Group` and a `Topic`
and also sends `Messages` to the same and/or other `Group` and `Topic` to inform other `Nodes`, what is happening.

## Messages
`Messages` are the media of communication between `Nodes`.
A `Message` contains a name for a `Callback` and the corresponding parameters.
A `Message` is send on a specific `Group` and `Topic`, so only `Callbacks` that subscribed to this `Group` and `Topic` will react.

## Callbacks
`Callbacks` are functions which serves as the reaction to a `Message` on a specific `Topic` in a specific `Group`.
Every `Callback` returns a `Message` to the `btNexus` with the name of the origin `Callback` + `_response`. So a `Node` can also subscribe to the response of the `Message` send out.

## Topics & Groups
`Topics` and `Groups` help to organize `Messages`. A `Callback` can only be mapped to one `Group` and  `Topic`.


# Prerequisites

* JRE installed
* Owner of a btNexus instance or a btNexus account

# Install btnexus-node-java

Clone the Repository and import it in intellij (https://www.jetbrains.com/idea/)
When you open the projects folder with intellij it asks you to link it to your gradle.
You can also open the build.gradle file. Intellij will ask you if you want to open `as file` or `as project`.
Choose `as project` here.
For the following question on the gradle settings choose the default settings(should be `use default gradle wrapper`).
To test if everything went well run the App under the test folder. (Right click on App.java -> Run App.main())
This should trigger the examples implemented in the test folder.
To run this test you need to set up the environment variables `AXON_HOST`, `TOKEN` and if you want a more verbose output `NEXUS_DEBUG`
* `AXON_HOST` is the url to your instance e.g. `company1.btnexus.ai/`
* `TOKEN` is the Access token
* If `NEXUS_DEBUG` is set to anything you will see a more verbose output.

This test should show a output like this:
```
Starting tests!
[ListeningNode] setUp
[SendingNode] setUp
[ListeningNode]: opened connection with DIRECT
[SendingNode]: opened connection with DIRECT
[ListeningNode]: Registered successfully
[SendingNode]: Registered successfully
[ListeningNode]: Joined Group: exampleGroup
[SendingNode]: Joined Group: exampleGroup
[SendingNode]: Subscribed to: ai.blackout.example
[ListeningNode]: Subscribed to: ai.blackout.example
[ListeningNode]: 50 || 15
[SendingNode]: 50:15
```
If the test went well you can build a fatJAR with gradle.
Open the Gradle view(on the right side) and under Tasks -> shadow run the task `shadowJar`
After these steps there should be a fatJAR in `btnexus-node-java/build/libs`.
This fatJAR can be included in your own Applications.

# Example Nodes
Following you will see an example of a Node which sends out the current minute
and second every five seconds.

```java
//System imports
import java.net.Proxy;
import java.net.URISyntaxException;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

//3rd party imports
import ai.blackout.node.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


/**
 * This Node shows how to implement an active Node which sends different Messages
 */
public class SendingNode extends Node{
    Thread sendThread;
    boolean shouldRun;
    /**
     * Constructor
     * @throws URISyntaxException
     */
    public SendingNode(String token, String axonURL, boolean debug) throws URISyntaxException {
        super( token,  axonURL,  debug);
    }

    /**
     * Constructor
     * @throws URISyntaxException
     */
    public SendingNode(String token, String axonURL, boolean debug, Proxy proxy) throws URISyntaxException {
        super( token,  axonURL,  debug, proxy);
    }

    /**
     * Setting up everything. This will be called everytime before the connection is established
     */
    @Override
    public void setUp(){
        super.setUp();
        Runnable exec = new Runnable() {
            /**
             * Sending current minute and second to the ListeningNode on the printTime and fuseTime callback.
             */
            @Override
            public void run() {
                while(shouldRun){   //  Make sure Thread terminates on reconnect
                    LocalTime time = java.time.LocalTime.now();
                    int min = time.getMinute();
                    int sec = time.getSecond();
                    JSONArray arrayParams = new JSONArray();
                    arrayParams.add(min);
                    arrayParams.add(sec);
                    JSONObject objectParams = new JSONObject();
                    objectParams.put("min", min);
                    objectParams.put("sec", sec);
                    try {
                        publish("exampleGroup", "example", "printTime", arrayParams);
                        publish("exampleGroup", "example", "fuseTime", objectParams);
                    } catch (NexusNotConnectedException e) {
                        onError(e);
                    }
                    try {
                        TimeUnit.SECONDS.sleep(5);
                    } catch (InterruptedException e) {
                        onError(e);
                    }
                }
            }
        };
        this.sendThread = new Thread(exec);
        this.shouldRun = true;
    }

    /**
     * Cleaning up everything. This will be called everytime after a disconnect happend
     * if not differently specified in onDisconnected()
     */
    @Override
    public void cleanUp(){
        super.cleanUp();
        try {
            this.shouldRun = false;
            this.sendThread.join();
        } catch (InterruptedException e) {
            onError(e);
        }

    }

    /**
     * Reacting to the fused Time with a print in a specific shape.
     *
     * @param params Parameters from the incoming Message
     * @return This Callback returns null
     */
    public String fuseTime_response(Object params) {
        JSONObject JSONparams = (JSONObject) params;        // You need to know what you take here JSONObject or JSONArray
        String fusedTime = (String) JSONparams.get("returnValue");
        System.out.println("[" + this.nodeName + "]: " + fusedTime);
        return null;
    }

    /**
     * This will be executed after the Node is successfully connected to the btNexus
     * Here you need to subscribe and start your Threads.
     */
    @Override
    public void onConnected() {
        try {
            this.subscribe("exampleGroup", "example", "fuseTime_response", this::fuseTime_response);
            sendThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```
The ListeningNode and all further examples can be seen in the test folder.


# Implement your own Node
Put the produced fatJAR in your External Libraries.
First you need know the purpose of your Node.
Nodes should be small and serve only one purpose.
To implement your own Node you need to inherit from the Node class,
implement your callbacks and if you are actively doing something implement your
Threads, that for example read in sensor data. See the examples to get started ;)

# Proxy Support
By default a Node uses the system proxy settings. 
If you want to use a specific proxy the Node can accept the proxy in the constructor:

```java
Proxy socksProxy = new Proxy(Proxy.Type.SOCKS,
        new InetSocketAddress("209.181.248.29",	9050));
testNode = new ListeningNode(token, axon, debug, socksProxy);
```
This was tested with SOCKS5 proxy
