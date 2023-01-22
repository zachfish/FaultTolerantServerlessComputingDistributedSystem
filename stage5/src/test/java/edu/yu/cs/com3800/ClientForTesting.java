package edu.yu.cs.com3800;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientForTesting {
    private LinkedBlockingQueue<Message> outgoingMessages;
    private LinkedBlockingQueue<Message> incomingMessages;
    private UDPMessageReceiver receiver;
    private UDPMessageSender sender;
    private InetSocketAddress myAddress;
    private int myPort;
    private int leaderPort;
    private String leaderHost;
    private int portsLength;



    public ClientForTesting(String host, int port, String leaderHost, int leaderPort){
        this.myPort = port;
        this.myAddress = new InetSocketAddress(host, port);
        this.outgoingMessages = new LinkedBlockingQueue<Message>();
        this.incomingMessages = new  LinkedBlockingQueue<Message>();

        this.sender = new UDPMessageSender(this.outgoingMessages, this.myPort);
        this.incomingMessages = new LinkedBlockingQueue<>();
        try {
            this.receiver = new UDPMessageReceiver(this.incomingMessages,null, this.myAddress, this.myPort, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.leaderHost = leaderHost;
        this.leaderPort = leaderPort;
    }



    public void sendManyMessages(String codeInput, int[]ports){
        this.portsLength = ports.length;
        for (int i = 0; i < ports.length; i++) {
            String code = codeInput.replace("world!", "world! code version " + i);
            try {
                sendMessage(code);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Util.startAsDaemon(sender, "Sender thread");
        Util.startAsDaemon(receiver, "Receiver thread");


    }

    public void sendMessage(String code) throws InterruptedException {
        Message msg = new Message(Message.MessageType.WORK, code.getBytes(), this.myAddress.getHostString(), this.myPort, this.leaderHost, this.leaderPort);
        this.outgoingMessages.put(msg);
    }

    public ArrayList<String> printResponses() throws Exception {
        ArrayList<String> responses = new ArrayList<>();
        System.out.println("responses: ");
        String completeResponse = "";
        for (int i = 0; i < this.portsLength; i++) {
            Message msg = this.incomingMessages.take();
            String response = new String(msg.getMessageContents());
            completeResponse += "Response #" + i + " from server" +msg.getSenderPort()+":  \n" + response + "\n";
            responses.add(completeResponse);
        }

        System.out.println(completeResponse);
        return responses;
    }

    public void shutdown(){
        this.sender.shutdown();
        this.receiver.shutdown();
    }







}
