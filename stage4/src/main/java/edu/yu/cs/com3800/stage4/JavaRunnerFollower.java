package edu.yu.cs.com3800.stage4;

import edu.yu.cs.com3800.JavaRunner;
import edu.yu.cs.com3800.LoggingServer;
import edu.yu.cs.com3800.Message;
import edu.yu.cs.com3800.Util;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;


public class JavaRunnerFollower extends Thread implements LoggingServer {
    private LinkedBlockingQueue<Message> outgoingMessages;
    private LinkedBlockingQueue<Message> incomingMessages;
    InetSocketAddress leaderAdress;
    InetSocketAddress serverAdress;
   // private boolean shutdown = false;
    private JavaRunner javaRunner;
    ServerSocket serverSocket;
    Logger logger;



    public JavaRunnerFollower(LinkedBlockingQueue<Message> incomingMessages, LinkedBlockingQueue<Message> outgoingMessages, InetSocketAddress server, InetSocketAddress leader){
        this.logger = initializeLogging(JavaRunnerFollower.class.getCanonicalName()+"-on-port-"+server.getPort());
        setName(getName()+"-port-"+server.getPort());
        setDaemon(true);
        this.incomingMessages =incomingMessages;
        this.outgoingMessages = outgoingMessages;
        this.serverAdress =server;
        this.leaderAdress = leader;
        try {
            this.serverSocket = new ServerSocket(this.serverAdress.getPort()+2);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public void shutdown() {
        interrupt();
    }


    @Override
    public void run(){
        logger.info("RunnerFollower on port "+this.serverAdress.getPort()+" has started");
        String responseString ="";
        try {
           javaRunner  = new JavaRunner();
        } catch (IOException e) {
            e.printStackTrace();
        }
        while(!this.isInterrupted()) {
           // Message message = incomingMessages.poll();
            Message message =null;
            try {
                Socket socket=  this.serverSocket.accept();
                byte[] data = Util.readAllBytesFromNetwork(socket.getInputStream());
                message = new Message(data);
                this.logger.fine("Received Message:\n"+message.toString());
               // System.out.println("in follower message type " + message.getMessageType() + "message: "+ new String(message.getMessageContents()));
            } catch (IOException e) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                e.printStackTrace(new PrintStream(bytes));
                String stackTrace = bytes.toString();
                this.logger.severe(e.getMessage() +"\n" + stackTrace);
                e.printStackTrace();
            }


            if (message != null) {
                byte[] contents = message.getMessageContents();

                try {
                //    System.out.println("Recieved: "+new String(contents, StandardCharsets.UTF_8));
                    responseString = javaRunner.compileAndRun(new ByteArrayInputStream(contents));
                    this.logger.fine("Response String Compiled: "+ responseString);

                } catch (Exception e) {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    e.printStackTrace(new PrintStream(bytes));
                    String stackTrace = bytes.toString();
                    responseString = e.getMessage() +"\n" + stackTrace ;
                    this.logger.severe(responseString);
                    e.printStackTrace();
                }

                //System.out.println("response: " + responseString);
                Message response = new Message(Message.MessageType.COMPLETED_WORK, responseString.getBytes(), serverAdress.getHostString(), serverAdress.getPort(), leaderAdress.getHostString(),leaderAdress.getPort()+2,message.getRequestID());
                sendBackToRoundRobin(response);


            }
        }

    }


    private void sendBackToRoundRobin(Message response){
        Socket socket = null;
        try {
            socket = new Socket(response.getReceiverHost(), response.getReceiverPort());
            OutputStream output = socket.getOutputStream();
            output.write(response.getNetworkPayload());
            this.logger.fine("Sent response back to RoundRobinLeader to port "+response.getReceiverPort());
        } catch (IOException e) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(bytes));
            String stackTrace = bytes.toString();
            this.logger.severe( e.getMessage() +"\n" + stackTrace);
            System.err.println(e.getMessage());

        }

    }


}

