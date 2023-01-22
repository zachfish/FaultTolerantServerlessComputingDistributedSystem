package edu.yu.cs.com3800;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ZooKeeperLeaderElection
{
    /**
     * time to wait once we believe we've reached the end of leader election.
     */
    private final static int finalizeWait = 200;
    private int waitTime;
    private Long proposedEpoch;
    private Long proposedLeader;
    private LinkedBlockingQueue<Message> incomingMessages;
    private ZooKeeperPeerServer myPeerServer;
    private Map<Long, ElectionNotification > votes;

    /**
     * Upper bound on the amount of time between two consecutive notification checks.
     * This impacts the amount of time to get the system up again after long partitions. Currently 60 seconds.
     */
    private final static int maxNotificationInterval = 60000;

    public ZooKeeperLeaderElection(ZooKeeperPeerServer server, LinkedBlockingQueue<Message> incomingMessages)
    {
        this.myPeerServer = server;
        this.incomingMessages = incomingMessages;
        this.votes = new HashMap<>();
        this.waitTime = finalizeWait;
    }


    private synchronized Vote getCurrentVote() {
        return new Vote(this.proposedLeader, this.proposedEpoch);
    }

    public synchronized Vote lookForLeader() {
        //send initial notifications to other peers to get things started
        this.proposedLeader = this.myPeerServer.getServerId();
        this.proposedEpoch = this.myPeerServer.getPeerEpoch();
        votes.put(this.myPeerServer.getServerId(), new ElectionNotification(this.proposedLeader, this.myPeerServer.getPeerState(), this.myPeerServer.getServerId(), this.proposedEpoch));
        sendNotifications();
        ElectionNotification vote = null;

        //Loop, exchanging notifications with other servers until we find a leader
        //Remove next notification from queue, timing out after 2 times the termination time
        //if no notifications received..
        //..resend notifications to prompt a reply from others..
        //.and implement exponential back-off when notifications not received..
        //if/when we get a message and it's from a valid server and for a valid server..

        while (this.myPeerServer.getPeerState().equals(ZooKeeperPeerServer.ServerState.LOOKING)) {

            try {
                vote = getNotificationFromMessage(this.incomingMessages.poll(waitTime, TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            if (vote == null) {
                if(waitTime <= maxNotificationInterval/2) {
                    waitTime = waitTime * 2;
                } else{
                    this.waitTime = maxNotificationInterval;
                }

                sendNotifications();
                continue;
            }

            Long newId = vote.getProposedLeaderID();
            Long newEpoch = vote.getPeerEpoch();
            votes.put(vote.getSenderID(), vote);

            switch (vote.getState()) {
                //switch on the state of the sender:
                case LOOKING://if the sender is also looking
                    //if the received message has a vote for a leader which supersedes mine, change my vote and tell all my peers what my new vote is.
                    //keep track of the votes I received and who I received them from.
                    //if I have enough votes to declare my currently proposed leader as the leader:
                    //first check if there are any new votes for a higher ranked possible leader before I declare a leader. If so, continue in my election loop
                    //If not, set my own state to either LEADING (if I won the election) or FOLLOWING (if someone lese won the election) and exit the election
                    if (supersedesCurrentVote(newId, newEpoch)) {
                        this.proposedLeader = newId;
                        this.proposedEpoch = newEpoch;
                        votes.put(this.myPeerServer.getServerId(), vote);
                        sendNotifications(); // let everyone know who new vote is

                    }
                    if (haveEnoughVotes(this.votes, getCurrentVote())) {
                        //checking one last time
                        Message message = checkForBetter();
                        if (message==null) {
                            return acceptElectionWinner(vote);
                        }
                    }
                    break;
                case FOLLOWING:
                case LEADING:
                    //if the sender is following a leader already or thinks it is the leader
                    //IF: see if the sender's vote allows me to reach a conclusion based on the election epoch that I'm in, i.e. it gives the majority to the vote of the FOLLOWING or LEADING peer whose vote I just received.
                    //if so, accept the election winner.
                    //As, once someone declares a winner, we are done. We are not worried about / accounting for misbehaving peers.
                    //ELSE:
                    // if n is from a LATER election epoch
                    //IF a quorum from that epoch are voting for the same peer as the vote of the FOLLOWING or LEADING peer whose vote I just received.
                    //THEN accept their leader, and update my epoch to be their epoch
                    //ELSE:
                    //keep looping on the election loop
                    //

                   /* if(supersedesCurrentVote(newId, newEpoch)){
                        this.proposedLeader = newId;
                        this.proposedEpoch = newEpoch;
                        votes.put(this.myPeerServer.getServerId(), vote);
                    }*/ //TODO do I need this?
                    if (haveEnoughVotes(this.votes, vote)){
                        return acceptElectionWinner(vote);
                    }
                    break;


                default:
                    break;


            }
        }

        return null; //TODO or should this be return vote
    }


    //my methods

    private Message getMessage(){
        Message message = null;
        try {
            message = this.incomingMessages.poll(this.waitTime, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null; //TODO this could be wrong

        }

        return message;
    }


    private Message checkForBetter(){
        Message message = getMessage();
        while (message!= null){
            ElectionNotification en = getNotificationFromMessage(message);
            if(supersedesCurrentVote(en.getProposedLeaderID(),en.getPeerEpoch())){
                try {
                    this.incomingMessages.put(message);
                } catch (InterruptedException e) {
                    System.out.println("here is the error: checkForBetter");
                    e.printStackTrace();
                }
                break;
            }
            message = getMessage();
        }

        return message; // this will be null if no better
    }

    //end my methods


    private void sendNotifications() {
        ElectionNotification en = new ElectionNotification(this.proposedLeader, this.myPeerServer.getPeerState(), myPeerServer.getServerId(), this.proposedEpoch);
        this.myPeerServer.sendBroadcast(Message.MessageType.ELECTION, buildMsgContent(en));
    }

    private Vote acceptElectionWinner(ElectionNotification n) {
        //set my state to either LEADING or FOLLOWING
        //clear out the incoming queue before returning
        if(n.getProposedLeaderID() == this.myPeerServer.getServerId()){
            this.myPeerServer.setPeerState(ZooKeeperPeerServer.ServerState.LEADING);
        }else{
            this.myPeerServer.setPeerState(ZooKeeperPeerServer.ServerState.FOLLOWING);
        }


        try {
            this.myPeerServer.setCurrentLeader(n);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.incomingMessages.clear();

        return n;
    }

    /*
     * We return true if one of the following three cases hold:
     * 1- New epoch is higher
     * 2- New epoch is the same as current epoch, but server id is higher.
     */
    protected boolean supersedesCurrentVote(long newId, long newEpoch) {
        return (newEpoch > this.proposedEpoch) || ((newEpoch == this.proposedEpoch) && (newId > this.proposedLeader));
    }
    /**
     * Termination predicate. Given a set of votes, determines if have sufficient support for the proposal to declare the end of the election round.
     * Who voted for who isn't relevant, we only care that each server has one current vote
     */
    protected boolean haveEnoughVotes(Map<Long, ElectionNotification > votes, Vote proposal) {
        //is the number of votes for the proposal > the size of my peer server’s quorum?
        int count =0;
        for (ElectionNotification en : this.votes.values()){
            if(proposal.getProposedLeaderID() == en.getProposedLeaderID()){
                count++;
            }
        }
        return count >= this.myPeerServer.getQuorumSize();
    }

    protected static ElectionNotification getNotificationFromMessage(Message received){
        if(received==null) return null;
        ByteBuffer msgBytes = ByteBuffer.wrap(received.getMessageContents());
        long leader = msgBytes.getLong();
        char stateChar = msgBytes.getChar();
        long senderID = msgBytes.getLong();
        long peerEpoch = msgBytes.getLong();
        ElectionNotification en = new ElectionNotification(leader, ZooKeeperPeerServer.ServerState.getServerState(stateChar), senderID, peerEpoch);
        return en;
    }

    protected static byte[] buildMsgContent(ElectionNotification notification) {
        if (notification==null) return null;
        ByteBuffer msgBytes  = ByteBuffer.allocate((Long.BYTES*3) +Character.BYTES);
        msgBytes.putLong(notification.getProposedLeaderID());
        msgBytes.putChar(notification.getState().getChar());
        msgBytes.putLong(notification.getSenderID());
        msgBytes.putLong(notification.getPeerEpoch());
        return msgBytes.array();
    }

}

