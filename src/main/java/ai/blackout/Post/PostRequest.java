package ai.blackout.Post;


import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;


import javax.net.ssl.HttpsURLConnection;
//import javax.xml.ws.http.HTTPException;
//import javax.xml.ws.http.HTTPException;

import ai.blackout.node.Callback;

/** Class       : PostRequest
 *  Description : A class to create Post requests. Each object is able to
 * */

public class PostRequest {



    /************ Class Variables **************/
    protected URL server;
    protected String headerId;
    protected String headerVal;
    protected JSONObject request;
    protected JSONObject response;
    protected HttpsURLConnection conn;
    protected Callback callback;


    /**************** Methods ****************/
    /** Constructor     : PostRequest
     *  @param headerId : Id for the Post Header
     *  @param headerVal: Value for the Post Header
     *  @param server   : the desired server to which the request is going to be sent
     *  @param request  : JSONObject, which holds the request
     *  @param callback : Callback to be executed upon receiving request
     **/
    public PostRequest(String headerId, String headerVal , String server, JSONObject request, Callback callback) throws MalformedURLException {
        this.headerId               = headerId;
        this.headerVal              = headerVal;
        this.request                = request;
        this.callback               = callback;
        this.server                 = new URL(server);
        this.request                = request;
        this.response               = null;
        this.conn                   = null;

    }




    /** Method          : send
     *  Description     : Sends the Post request with the currently set parameters and executes the callback
     * */
    public void send() throws IOException, ParseException, HTTPExceptionWithReason {
        this.conn = (HttpsURLConnection) this.server.openConnection();
        //Set request parameters and headers
        this.conn.setRequestMethod("POST");
        this.conn.setRequestProperty(this.headerId, this.headerVal);
        //this.conn.setRequestProperty("User-Agent", this.userAgent);
        this.conn.setRequestProperty("Content-Type", "application/json");
        this.conn.setRequestProperty("Accept", "application/json");
        this.conn.setDoOutput(true);
        this.conn.setDoInput(true);
        //Open connection to server
        this.conn.connect();
        //Send request
        OutputStream os = this.conn.getOutputStream();
        os.write(this.request.toString().getBytes());
        os.flush();
        os.close();
        //If request went through

        if (conn.getResponseCode() == (HttpsURLConnection.HTTP_OK)) {
            //read response contents
            BufferedReader in = new BufferedReader(new InputStreamReader(this.conn.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            //crate JSONObject from String response
            JSONParser pars = new JSONParser();
            this.response = (JSONObject) pars.parse(response.toString());
            callback.apply(this.response);
            //Disconnect from server
            conn.disconnect();
        } else {
            BufferedReader in = new BufferedReader(new InputStreamReader(this.conn.getErrorStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            //crate JSONObject from String response
            JSONParser pars = new JSONParser();
            JSONObject errorResponse;
            errorResponse = (JSONObject) pars.parse(response.toString());
            //Disconnect from server
            conn.disconnect();
            throw new HTTPExceptionWithReason((String)errorResponse.get("error"));

            //throw new HTTPExceptionWithReason(conn.getResponseCode(), (String)errorResponse.get("error"));
        }

    }
}
