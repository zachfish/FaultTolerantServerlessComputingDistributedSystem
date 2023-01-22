package edu.yu.cs.com3800;

import edu.yu.cs.com3800.stage1.Client;
import edu.yu.cs.com3800.stage1.ClientImpl;
import edu.yu.cs.com3800.stage5.GatewayServer;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Stage1Test {
   // String myIp = "192.168.1.159";
    String basicCode =  "public class Test{ public String run(){ return \"Hello Server\";}}";




    @BeforeClass
    public static void startServer(){
        Map<Long, InetSocketAddress > peerIDtoAddress = new HashMap<>();
        System.out.println("Log files are in stage1 root directory");
        GatewayServer myserver = null;
        myserver = new GatewayServer(9000, 800L,peerIDtoAddress);
        myserver.start();


    }


    @Test
    public void basicTestClientServer() {
        ClientImpl myClient = null;
        try {
            myClient =  new ClientImpl("localhost", 9000);
            myClient.sendCompileAndRunRequest(this.basicCode);
            System.out.println("Expected Response:");
            System.out.println("Hello Server");
            System.out.println("Actual Response:");
            System.out.println(myClient.getResponse().getBody());
            assertEquals("Hello Server",myClient.getResponse().getBody());
            assertEquals(200, myClient.getResponse().getCode());

        } catch(Exception e) {
            System.err.println(e.getMessage());

        }
    }

    @Test
    public void anotherBasicTest(){
        ClientImpl myClient = null;
        try {
            myClient =  new ClientImpl("localhost", 9000);
            myClient.sendCompileAndRunRequest("public class Test{ public String run(){ return \"This is working!!!!\";}}");
            System.out.println("Expected Response:");
            System.out.println("This is working!!!!");
            System.out.println("Actual Response:");
            System.out.println(myClient.getResponse().getBody());
            assertEquals("This is working!!!!",myClient.getResponse().getBody());
            assertEquals(200, myClient.getResponse().getCode());

        } catch(Exception e) {
            System.err.println(e.getMessage());

        }

    }

    @Test
    public void errorInSourceCode() throws IOException {
        ClientImpl myClient = null;
        myClient =  new ClientImpl("localhost", 9000);
        myClient.sendCompileAndRunRequest("This is some bad code, it should not compile");
        assertEquals(400, myClient.getResponse().getCode());
        assertTrue(myClient.getResponse().getBody().contains("No class name found in code"));
    }


    @Test(expected = IllegalArgumentException.class)
    public void nullSourceCode() throws IOException {
        ClientImpl myClient = null;
        myClient =  new ClientImpl("localhost", 9000);
        myClient.sendCompileAndRunRequest(null);
        myClient.getResponse().getCode();

    }

    @Test
    public void badClientWrongContentType() throws IOException {
        BadClientImpl myClient = null;
        myClient =  new BadClientImpl("localhost", 9000, "Content-Type", "python");
        myClient.sendCompileAndRunRequest(this.basicCode);
        assertEquals(400, myClient.getResponse().getCode());
        assertEquals("", myClient.getResponse().getBody());
    }

    @Test
    public void badClientNoContentKey() throws IOException {
        BadClientImpl myClient = null;
        myClient =  new BadClientImpl("localhost", 9000, "City", "Los Angeles");
        myClient.sendCompileAndRunRequest(this.basicCode);
        assertEquals(400, myClient.getResponse().getCode());
        assertEquals("", myClient.getResponse().getBody());

    }

    @Test
    public void badClientGetRequest() throws IOException{
        BadClientImpl myClient = null;
        myClient =  new BadClientImpl("localhost", 9000, "City", "Los Angeles");
        myClient.sendGetRequest();
        assertEquals(405, myClient.getResponse().getCode());
        assertEquals("", myClient.getResponse().getBody());
    }










    private class BadClientImpl implements Client {
        HttpClient client;
        URI uri;
        HttpResponse<String> response;
        String contentKey;
        String contentValue;

        public BadClientImpl(String hostName, int hostPort, String contentKey, String contentValue) throws MalformedURLException {
            this.client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .build();
            this.uri =  URI.create("http://" + hostName + ":" + hostPort + "/compileandrun");
            this.contentKey= contentKey;
            this.contentValue= contentValue;

        }


        public void sendGetRequest( ){
            HttpRequest request =  HttpRequest.newBuilder()
                    .GET()
                    .uri(this.uri)
                    .setHeader(this.contentKey,this.contentValue)
                    .build();


            try {
                this.response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e){

            }
        }



        @Override
        public void sendCompileAndRunRequest(String src) throws IOException {

            if(src == null) throw new IllegalArgumentException();

            HttpRequest request =  HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(src))
                    .uri(this.uri)
                    .setHeader(this.contentKey,this.contentValue)
                    .build();
            try {
                this.response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e){

            }
        }

        @Override
        public Response getResponse() throws IOException {
            Response response = new Response(this.response.statusCode(), this.response.body());
            return response;
        }




    }




}
