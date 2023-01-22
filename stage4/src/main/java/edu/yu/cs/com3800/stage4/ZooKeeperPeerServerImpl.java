package edu.yu.cs.com3800.stage4;

import edu.yu.cs.com3800.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.yu.cs.com3800.Message;


public class ZooKeeperPeerServerImpl extends Thread implements ZooKeeperPeerServer, LoggingServer {
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
    private RoundRobinLeader roundRobinLeader;
    private JavaRunnerFollower javaRunnerFollower;
    ServerSocket serverSocket;
    private Logger log;


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
        this.log = initializeLogging(ZooKeeperPeerServerImpl.class.getCanonicalName() + "-with-id-" + id + "-on-port-"+myPort);
        this.setName("ZooKeeperPeerServerImpl-on-port-"+this.myPort);

    }


    public ZooKeeperPeerServerImpl(int myPort, long peerEpoch, Long id, Map<Long,InetSocketAddress> peerIDtoAddress, ServerState state){
        //code here...
        this.myPort = myPort;
        this.state = state;
        this.peerEpoch = peerEpoch;
        this.outgoingMessages = new LinkedBlockingQueue<Message>();
        this.incomingMessages = new LinkedBlockingQueue<Message>();
        this.id = id;
        this.peerEpoch = peerEpoch;
        this.peerIDtoAddress = peerIDtoAddress;
        this.myAddress = new InetSocketAddress("localhost", myPort);
        this.currentLeader = new Vote(id, peerEpoch);
        this.log = initializeLogging(ZooKeeperPeerServerImpl.class.getCanonicalName() + "-with-id-" + id + "-on-port-"+myPort);
        this.setName("ZooKeeperPeerServerImpl-on-port-"+this.myPort);
    }


    @Override
    public void shutdown(){
        this.shutdown = true;
        this.senderWorker.shutdown();
        this.receiverWorker.shutdown();
        if(this.roundRobinLeader!=null) this.roundRobinLeader.shutdown();
        if(this.javaRunnerFollower!= null) this.javaRunnerFollower.shutdown();
    }

    @Override
    public void setCurrentLeader(Vote v) throws IOException {
        this.currentLeader = v;
        this.peerEpoch = v.getPeerEpoch();
        this.log.info("Set Leader to server on port " + this.peerIDtoAddress.get(v.getProposedLeaderID()));
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
        return ((peerIDtoAddress.size())/2)+1;
    }//used to be peerIDtoAddress.size()+1)/2 +1

    @Override
    public void run(){
        //step 1: create and run thread that sends broadcast messages
        //step 2: create and run thread that listens for messages sent to this server
        //step 3: main server loop
        log.log(Level.FINE, "started: "+ this.getName());
        try{ //todo this all may be very wrong
            LinkedBlockingQueue<Message> tcpMessages = new LinkedBlockingQueue<>() ;
            this.senderWorker = new UDPMessageSender(this.outgoingMessages,this.myPort);
            this.receiverWorker= new UDPMessageReceiver(this.incomingMessages, this.myAddress, this.myPort, this);

            Util.startAsDaemon(this.senderWorker, "Sender");
            Util.startAsDaemon(this.receiverWorker, "Receiver");

            //this.senderWorker.start();
            //this.receiverWorker.start();

            while (!this.shutdown){
                switch (getPeerState()){

                    case OBSERVER:
                    case LOOKING:
                        //start leader election, set leader to the election winner
                        ZooKeeperLeaderElection election = new ZooKeeperLeaderElection(this, this.incomingMessages);
                        setCurrentLeader(election.lookForLeader());
                        break;
                    case LEADING:
                        if(roundRobinLeader == null){//todo check this
                            this.roundRobinLeader = new RoundRobinLeader(this.peerIDtoAddress,  tcpMessages, this.outgoingMessages, this.myAddress);
                            Util.startAsDaemon(this.roundRobinLeader, "roundRobinLeader");
                            this.log.info("Created RoundRobinLeader");
                        }
                        break;
                    case FOLLOWING:
                        if(this.javaRunnerFollower == null) {
                            InetSocketAddress leader = peerIDtoAddress.get(this.currentLeader.getProposedLeaderID());
                            this.javaRunnerFollower = new JavaRunnerFollower(this.incomingMessages, this.outgoingMessages, this.myAddress, leader); //todo not sure??
                            Util.startAsDaemon(this.javaRunnerFollower, "javaFollower");
                            this.log.info("Created JavaRunnerFollower");
                        }
                        break;


                }
            }
        }
        catch (Exception e) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(bytes));
            String stackTrace = bytes.toString();
            this.log.severe(e.getMessage() +"\n" + stackTrace);
            e.printStackTrace();
        }
    }

}
