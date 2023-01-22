package edu.yu.cs.com3800.stage5;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import edu.yu.cs.com3800.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;
import java.util.logging.SocketHandler;

/**
 *
 * The Gateway must be implemented in a class called GatewayServer which…
 * • …creates an HTTPServer on whatever port number is passed to it in its constructor
 * • …keeps track of what node is currently the leader and sends it all client requests over a TCP connection
 * • …is a peer in the ZooKeeper cluster, but as an OBSERVER only –
 * it does not get a vote in leader elections, rather it merely observes and watches for a winner so it
 * knows who the leader is to which it should send client requests.
 *
 *
 * -An OBSERVER must never change its state to any other ServerState
 *
 * - Other nodes must not count an OBSERVER when determining how many votes are needed for a quorum,
 *  and must not count any votes sent by an OBSERVER when
 *  determining if there is a quorum voting for a given server
 *
 * - Think though carefully what changes this will require to ZooKeeperLeaderElection!
 * • …will have a number of threads running:
 * o an HttpServer to accept client requests
 * o a GatewayPeerServerImpl, which is a subclass of ZooKeeperPeerServerImpl which can only be an OBSERVER
 * o for every client connection, the HttpServer creates and runs a thread in which it calls an HttpHandler,
 * which will, in turn, synchronously communicate with the master/leader over TCP to submit the client
 * request and get a response. Be careful to not have any instance variables
 * in your HttpHandler – its methods must be thread safe! Only use local variables in your methods.
 *
 *
 */


public class GatewayServer extends Thread implements LoggingServer {
    GatewayPeerServerImpl gpsi;
    SocketHandler handler;
    ServerSocket serverSocket;
    Map<Long, InetSocketAddress> peerIDtoAddress;
    //static Logger log = Logger.getLogger(SimpleServerImpl.class.getName());
    //static FileHandler fileHandler;

    final int port;
    Long reqId =0L;
    HttpServer httpServer;
    private LinkedBlockingQueue<Message> messagesFromClient = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<byte[]> bytesFromClient = new LinkedBlockingQueue<>();
    HttpClient httpClient;
    URI uri;
    private Logger logger;
    //FileHandler fileHandler;

    //private int leaderPort;
    //private String leaderHost;


    public GatewayServer(int port, Long id, Map<Long, InetSocketAddress> peerIDtoAddress){
        this.logger= initializeLogging(GatewayServer.class.getCanonicalName() + "-on-port-" + port);
        setName(GatewayServer.class.getCanonicalName() + "-on-port-" + port);
        this.port = port;
        this.gpsi = new GatewayPeerServerImpl(port+2, 0, id, peerIDtoAddress); //todo have to fix leader election to a accomadate , also what is proper port??
        this.peerIDtoAddress=peerIDtoAddress;
        try {
            this.serverSocket= new ServerSocket(port+4);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public GatewayPeerServerImpl getServer(){
        return this.gpsi;
    }


    @Override
    public void run() {
        this.logger.fine("Started "+getName());
        //Thread HTTPThread = new Thread(() -> { //todo maybe don't need this thread
            try {//HTTP Server
                this.httpServer=  HttpServer.create(new InetSocketAddress(port), 0);
                this.httpServer.createContext("/compileandrun", new Handler());
                this.httpServer.setExecutor(null); // creates a default executor
                this.httpServer.start();
                sleep(3000);
            } catch (Exception e) {
               // e.printStackTrace();
                System.err.println(e.getMessage());
            }
      //  });
       // HTTPThread.start();


        while(!this.isInterrupted()) {
            //getting the leader
            //Message message = messagesFromClient.poll(); //messages are added here from httphandler below :)
            byte[] bytes = bytesFromClient.poll();


            if (bytes != null) {
                this.logger.fine("Received Work from Client via HTTP:\n" + new String(bytes));

                InetSocketAddress leader = getLeader();
                this.logger.fine("Leader is on port: "+ leader.getPort());
                Socket socket = null;
                //now connecting to Socket that should be in ROundRobin for the leader

                try {
                    Message message = new Message(Message.MessageType.WORK, bytes, "localhost", this.port+2,leader.getHostName(), leader.getPort() + 2);
                    int leaderTCP = leader.getPort() + 3;
                    socket = new Socket(leader.getHostName(),leaderTCP);
                    OutputStream output = socket.getOutputStream();
                    output.write(message.getNetworkPayload());
                    this.logger.fine("Sent tcp message to " + leader.getPort()+2 + "\n"+message.toString());


                } catch (IOException e) {
                    ByteArrayOutputStream bytes1 = new ByteArrayOutputStream();
                    e.printStackTrace(new PrintStream(bytes1));
                    String stackTrace = bytes1.toString();
                    this.logger.severe(e.getMessage() +"\n" + stackTrace);
                    e.printStackTrace();
                    System.err.println(e.getMessage());

                }


            }

        }


    }


    private void addMessage(byte[] b){
        bytesFromClient.offer(b);

    }

    private InetSocketAddress getLeader(){
        return peerIDtoAddress.get(this.gpsi.getCurrentLeader().getProposedLeaderID());
    }

    private int getPort(){
        return this.port;
    }




    class Handler implements HttpHandler {


        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] buffer = exchange.getRequestBody().readAllBytes();
            String stringBody = new String(buffer);
            logger.info("Java Code Input:) " + stringBody);
            InputStream stream = new ByteArrayInputStream(buffer);
            JavaRunner jr = new JavaRunner();
            String response = "";
            Long requestId = reqId++;


            if(exchange.getRequestMethod().equals("GET")){
                exchange.sendResponseHeaders(405, 0);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                logger.info("Status Code: " + 405);
                logger.info("Error: GET Requests are Not Valid");
                logger.info("Output:       ");
                os.close();
                exchange.close();
            }

            if(!exchange.getRequestHeaders().containsKey("Content-Type") || !(exchange.getRequestHeaders().get("Content-Type").get(0).equals("text/x-java-source"))){
                exchange.sendResponseHeaders(400, 0);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                logger.info("Status Code: " + 400);
                logger.info("Error: No Proper Content-Type Key-Mapping");
                logger.info("Output:       ");
                os.close();
                exchange.close();

            } else{

                try { //todo: this is where the work gets done!!!
                   // byte[] bytes = buffer;
                   // bytesFromClient.offer(buffer);
                    //Message message = new Message(Message.MessageType.WORK,buffer,"localhost", getPort(), getLeader().getHostString(), getLeader().getPort(),requestId);

                    addMessage(buffer);
                    //receiveResponse();
                    //Thread tcpServerReceiveThread = new Thread(() -> {
                        Socket socketAccepted = null;


                        try {
                            socketAccepted = serverSocket.accept();
                            byte[] data = Util.readAllBytesFromNetwork(socketAccepted.getInputStream());
                            Message message = new Message(data);
                            logger.fine("Received responce via TCP:\n"+ message.toString());
                            //sends back to client
                            byte[] responseBytes = message.getMessageContents();
                            exchange.sendResponseHeaders(200, response.length()); //todo response???
                            OutputStream  os = exchange.getResponseBody();
                            os.write(responseBytes);
                            logger.fine("Sent Response back to Client via HTTP: " + new String(responseBytes));
                            //responseToReturn =response;

                        }catch(IOException e){
                            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                            e.printStackTrace(new PrintStream(bytes));
                            String stackTrace = bytes.toString();
                            logger.severe(e.getMessage() +"\n" + stackTrace);
                            e.printStackTrace();
                            System.err.println(e.getMessage());
                        }

                    exchange.close();

                } catch (Exception e) { //do better here
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    e.printStackTrace(new PrintStream (bytes));
                    String stackTrace = bytes.toString();
                    response = e.getMessage() +"\n" + stackTrace ;
                    exchange.sendResponseHeaders(400, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    logger.info("Status Code: " + 400);
                    logger.info("Output: " + new String(response.getBytes()));
                    os.close();
                    exchange.close();
                }
            }



        }
    }



}



