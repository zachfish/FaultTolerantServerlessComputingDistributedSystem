package edu.yu.cs.com3800.stage2;

import edu.yu.cs.com3800.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;


public class ZooKeeperPeerServerImpl extends Thread implements ZooKeeperPeerServer {
    private final InetSocketAddress myAddress;
    private final int myPort;
    private ServerState state;
    private volatile boolean shutdown;
    private LinkedBlockingQueue<Message> outgoingMessages;
    private LinkedBlockingQueue<Message> incomingMessages;
    private Long id;
    private long peerEpoch;
    private volatile Vote currentLeader;
    private Map<Long,InetSocketAddress> peerIDtoAddress;
    private UDPMessageSender senderWorker;
    private UDPMessageReceiver receiverWorker;


    public ZooKeeperPeerServerImpl(int myPort, long peerEpoch, Long id, Map<Long,InetSocketAddress> peerIDtoAddress){
        //code here...
        this.myPort = myPort;
        this.state = ServerState.LOOKING;
        this.peerEpoch = peerEpoch;
        this.outgoingMessages = new LinkedBlockingQueue<Message>();
        this.incomingMessages = new LinkedBlockingQueue<Message>();
        this.id = id;
        this.peerEpoch = peerEpoch;
        this.peerIDtoAddress = peerIDtoAddress;
        this.myAddress = new InetSocketAddress("localhost", myPort);
        this.currentLeader = new Vote(id, peerEpoch);

    }

    @Override
    public void shutdown(){
        this.shutdown = true;
        this.senderWorker.shutdown();
        this.receiverWorker.shutdown();
    }

    @Override
    public void setCurrentLeader(Vote v) throws IOException {
        this.currentLeader = v;
        this.peerEpoch = v.getPeerEpoch();
    }

    @Override
    public Vote getCurrentLeader() {
        return this.currentLeader;
    }

    @Override
    public void sendMessage(Message.MessageType type, byte[] messageContents, InetSocketAddress target) throws IllegalArgumentException {
        Message message = new Message(type, messageContents, this.myAddress.getHostString(), this.myPort, target.getHostString(), target.getPort());
        this.outgoingMessages.offer(message);

    }

    @Override
    public void sendBroadcast(Message.MessageType type, byte[] messageContents) {
        for (InetSocketAddress address : peerIDtoAddress.values()) {
            if (address != this.myAddress) {
                sendMessage(type, messageContents, address);
            } //TODO Maybe do need this???

        }
    }

    @Override
    public ServerState getPeerState() {
        return this.state;
    }

    @Override
    public void setPeerState(ServerState newState) {
        this.state = newState;
    }

    @Override
    public Long getServerId() {
        return this.id;
    }

    @Override
    public long getPeerEpoch() {
        return this.peerEpoch;
    }

    @Override
    public InetSocketAddress getAddress() {
        return this.myAddress;
    }

    @Override
    public int getUdpPort() {
        return this.myPort;
    }

    @Override
    public InetSocketAddress getPeerByID(long peerId) {
        return peerIDtoAddress.get(peerId);
    }

    @Override
    public int getQuorumSize() {
        return (peerIDtoAddress.size()+1)/2 +1;
    }

    @Override
    public void run(){
        //step 1: create and run thread that sends broadcast messages
        //step 2: create and run thread that listens for messages sent to this server
        //step 3: main server loop
        try{
            this.senderWorker = new UDPMessageSender(this.outgoingMessages,this.myPort);
            this.receiverWorker= new UDPMessageReceiver(this.incomingMessages, this.myAddress, this.myPort, this);
            this.senderWorker.start();
            this.receiverWorker.start();

            while (!this.shutdown){
                switch (getPeerState()){
                    case LOOKING:
                        //start leader election, set leader to the election winner
                        ZooKeeperLeaderElection election = new ZooKeeperLeaderElection(this, this.incomingMessages);
                        setCurrentLeader(election.lookForLeader());
                        break;
                }
            }
        }
        catch (Exception e) {
           //code... //TODO ???
        }
    }

}
