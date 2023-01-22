package edu.yu.cs.com3800.stage3;

import edu.yu.cs.com3800.JavaRunner;
import edu.yu.cs.com3800.Message;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;


public class JavaRunnerFollower extends Thread{
    private LinkedBlockingQueue<Message> outgoingMessages;
    private LinkedBlockingQueue<Message> incomingMessages;
    InetSocketAddress leaderAdress;
    InetSocketAddress serverAdress;
   // private boolean shutdown = false;
    private JavaRunner javaRunner;



    public JavaRunnerFollower(LinkedBlockingQueue<Message> incomingMessages, LinkedBlockingQueue<Message> outgoingMessages, InetSocketAddress server, InetSocketAddress leader){
        setDaemon(true);
        this.incomingMessages =incomingMessages;
        this.outgoingMessages = outgoingMessages;
        this.serverAdress =server;
        this.leaderAdress = leader;

    }
    public void shutdown() {
        interrupt();
    }


    @Override
    public void run(){
        String responseString ="";
        try {
           javaRunner  = new JavaRunner();
        } catch (IOException e) {
            e.printStackTrace();
        }
        while(!this.isInterrupted()) {
            Message message = incomingMessages.poll();
            if (message != null) {
                byte[] contents = message.getMessageContents();

                try {
                //    System.out.println("Recieved: "+new String(contents, StandardCharsets.UTF_8));
                    responseString = javaRunner.compileAndRun(new ByteArrayInputStream(contents));

                } catch (Exception e) {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    e.printStackTrace(new PrintStream(bytes));
                    String stackTrace = bytes.toString();
                    responseString = e.getMessage() +"\n" + stackTrace ;
                    e.printStackTrace();
                }

                Message response = new Message(Message.MessageType.COMPLETED_WORK, responseString.getBytes(), serverAdress.getHostString(), serverAdress.getPort(), leaderAdress.getHostString(),leaderAdress.getPort(),message.getRequestID());
              //  System.out.println("Response by server " + this.serverAdress.getPort() + " " +" to port: " + leaderAdress.getPort() + " " + new String(response.getMessageContents(), StandardCharsets.UTF_8));
                this.outgoingMessages.offer(response);

            }
        }

    }


}

