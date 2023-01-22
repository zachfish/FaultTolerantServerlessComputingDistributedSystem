package edu.yu.cs.com3800;


import edu.yu.cs.com3800.stage5.ZooKeeperPeerServerImpl;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

public class JudahsDemo {
    private String validClass = "package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world!\";\n    }\n}\n";

    private LinkedBlockingQueue<Message> outgoingMessages;
    private LinkedBlockingQueue<Message> incomingMessages;
    private int[] ports = {8010, 8020, 8030, 8040, 8050, 8060, 8070, 8080};
    //private int[] ports = {8010, 8020};
    private int leaderPort = this.ports[this.ports.length - 1];
    private int myPort = 9998;
    private InetSocketAddress myAddress = new InetSocketAddress("localhost", this.myPort);
    private ArrayList<ZooKeeperPeerServer> servers;
    UDPMessageSender sender;
    UDPMessageReceiver receiver;
 //   int offset =0;



    public JudahsDemo() throws Exception {
        this.outgoingMessages = new LinkedBlockingQueue<>();
        this.sender = new UDPMessageSender(this.outgoingMessages, this.myPort);
        this.incomingMessages = new LinkedBlockingQueue<>();
        this.receiver = new UDPMessageReceiver(this.incomingMessages, null, this.myAddress, this.myPort, null);

    }

    public ArrayList<String> basicTest() throws Exception{
        createServers();
        //step2.1: wait for servers to get started
        try {
            Thread.sleep(6000);
        }
        catch (Exception e) {
        }
        //printLeaders();
        //step 3: since we know who will win the election, send requests to the leader, this.leaderPort
        for (int i = 0; i < this.ports.length; i++) {
            String code = this.validClass.replace("world!", "world! from code version " +i);
            sendMessage(code);
        }
        Util.startAsDaemon(sender, "Sender thread");
        Util.startAsDaemon(receiver, "Receiver thread");

        ArrayList<String> answer = printResponses();
        stopServers();
        shutdown();

        return answer;
    }




    private void printLeaders() {
        for (ZooKeeperPeerServer server : this.servers) {
            Vote leader = server.getCurrentLeader();
            if (leader != null) {
                System.out.println("Server on port " + server.getAddress().getPort() + " whose ID is " + server.getServerId() + " has the following ID as its leader: " + leader.getProposedLeaderID() + " and its state is " + server.getPeerState().name());
            }

        }
    }

    protected void stopServers() {
        for (ZooKeeperPeerServer server : this.servers) {
            server.shutdown();
        }
    }

    private ArrayList<String> printResponses() throws Exception {
        ArrayList<String> responses = new ArrayList<>();
        System.out.println("responses: ");
        String completeResponse = "";
        for (int i = 0; i < this.ports.length; i++) {
            Message msg = this.incomingMessages.take();
            String response = new String(msg.getMessageContents());
            completeResponse += "Response #" + i + ":\n" + response + "\n";
            responses.add(completeResponse);
        }
        System.out.println(completeResponse);
        return responses;
    }

    private void sendMessage(String code) throws InterruptedException {
        Message msg = new Message(Message.MessageType.WORK, code.getBytes(), this.myAddress.getHostString(), this.myPort, "localhost", this.leaderPort);
        this.outgoingMessages.put(msg);
    }
    private void createServers() {
        //create IDs and addresses
        HashMap<Long, InetSocketAddress> peerIDtoAddress = new HashMap<>(8);
        for (int i = 0; i < this.ports.length; i++) {
            peerIDtoAddress.put(Integer.valueOf(i).longValue(), new InetSocketAddress("localhost", this.ports[i]));
        }
        //create servers
        this.servers = new ArrayList<>(3);
        for (Map.Entry<Long, InetSocketAddress> entry : peerIDtoAddress.entrySet()) {
            HashMap<Long, InetSocketAddress> map = (HashMap<Long, InetSocketAddress>) peerIDtoAddress.clone();
            map.remove(entry.getKey());
            ZooKeeperPeerServerImpl server = new ZooKeeperPeerServerImpl(entry.getValue().getPort(), 0, entry.getKey(), map);
            this.servers.add(server);
          //  System.out.println("here"+ server.getAddress().getPort());
            new Thread(server, "Server on port " + server.getAddress().getPort()).start();
        }
    }

    public void shutdown(){
        this.sender.shutdown();
        this.receiver.shutdown();
    }

    public static void main(String[] args) throws Exception {
        //new edu.yu.cs.com3800.stage4.Stage3PeerServerDemo();
    }
}
