package edu.yu.cs.com3800.stage5;

import edu.yu.cs.com3800.LoggingServer;
import edu.yu.cs.com3800.Message;
import edu.yu.cs.com3800.Util;
import edu.yu.cs.com3800.ZooKeeperPeerServer;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HeartbeatMonitor extends Thread implements LoggingServer {

    static final int GOSSIP = 3000;
    static final int FAIL = GOSSIP * 10;
    static final int CLEANUP = FAIL * 2;
    private int port;
    private ZooKeeperPeerServerImpl server;
    private Map<Long, Gossip> gossipInfoMap  = new ConcurrentHashMap<>();
    LinkedBlockingQueue<Message> incomingGossip;
    private ArrayList<Gossip> recievedGossip = new ArrayList<>();
    protected volatile Long hearbeatCounter =0L;
    private Map<Long,InetSocketAddress> peerIDtoAddress;

    protected HashSet<Long> failed = new HashSet<>();
    protected HashSet<Long> deleted = new HashSet<>();
    boolean shutdown = false;
    private Logger log;

    private Sender sender;
    private Deleter deleter;
    private Receiver receiver;




    public HeartbeatMonitor(int port, ZooKeeperPeerServerImpl server, LinkedBlockingQueue<Message> incomingGossip){
        this.port = port;
        this.server = server;
        this.incomingGossip = incomingGossip;
        this.peerIDtoAddress = this.server.getPeerIDtoAddress();
        this.log = initializeLogging(HeartbeatMonitor.class.getCanonicalName() + "-with-id-" + server.getServerId() + "-on-port-"+port);
        this.setName("HeartBeatMonitor-on-port-"+this.port);


        for(Map.Entry<Long, InetSocketAddress>  a : peerIDtoAddress.entrySet()) {
            long localId = a.getKey();
            Gossip gossipInfo = new Gossip(System.currentTimeMillis(),0, localId);
            gossipInfoMap.put(localId, gossipInfo);
        }




    }


    protected HashSet<Long> getFailedServers(){
        return this.failed;
    }



    @Override
    public void run(){
        this.receiver = new Receiver();
        this.deleter = new Deleter();
        this.sender = new Sender();
        Util.startAsDaemon(this.receiver, "Gossip Receiver");
        Util.startAsDaemon(this.sender, "Gossip Sender");
        Util.startAsDaemon(this.deleter, "Deleter");

    }


    public void shutdown(){
        this.sender.interrupt();
        this.receiver.interrupt();
        this.receiver.shutdown();
        this.deleter.interrupt();
        interrupt();
        this.shutdown =true;
    }





    private class Sender extends Thread{

        @Override
        public void run(){

            while(!shutdown) {
                sendGossip();//checkfordead, increment heartbear count

                try {
                    Thread.sleep(GOSSIP);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }

        private void sendGossip(){
            hearbeatCounter++;
            String stringMap = server.getServerId() + "-";
            //System.out.println(stringMap);
            for(Map.Entry<Long, InetSocketAddress>  a : peerIDtoAddress.entrySet()){
                long localId = a.getKey();
                if(failed.contains(localId)) continue;
                Gossip gossipInfo = gossipInfoMap.get(localId);
                Long counter = gossipInfo.counter;
                if(localId == server.getServerId()){
                    counter = hearbeatCounter;
                }
                stringMap += localId + ":" + counter +"/";
            }


            stringMap += server.getServerId()+":"+hearbeatCounter+"/";


           // System.out.println(stringMap);


            byte[] bytes = stringMap.getBytes();
            Map<Long, InetSocketAddress> peerIDtoAddress = server.getPeerIDtoAddress();

            Random generator = new Random();
            Object[] values = peerIDtoAddress.values().toArray();
            InetSocketAddress randomServer = (InetSocketAddress) values[generator.nextInt(values.length)];
            server.sendMessage(Message.MessageType.GOSSIP, bytes, randomServer);

        }

    }


    private class Receiver extends Thread{

        @Override
        public void run(){
            while(!shutdown){

                try{
                    Message m = incomingGossip.poll();
                    if (m==null) continue;
                    receiveGossip(m);
                } catch (Exception e){

                }
            }

        }



        public void shutdown(){
            interrupt();

        }


        private void receiveGossip(Message message){
            Long time = System.currentTimeMillis();
            if (message.getMessageContents()==null) return;

            String string = new String(message.getMessageContents());
           // System.out.println("String: " + string);


            String[] split0 = string.split("-");
            Long senderId = Long.valueOf(split0[0]);



            String[] split = split0[1].split("/");
            //System.out.println(Arrays.toString(split));

            for(String s : split){
                String[] entry = s.split(":");
                Long localId = Long.valueOf(entry[0]);
                if(failed.contains(localId)) continue; //if failed want to not listen to it
                Long counter = Long.parseLong(entry[1]);

            //    System.out.println(localId+ "  " + counter);
                if(counter > hearbeatCounter){
                    hearbeatCounter = counter;
                }


                Gossip g = gossipInfoMap.get(localId);
                if (g==null){
                    g = new Gossip(time, counter, senderId);
                } else{
                    if(counter > g.counter){
                        g.counter = counter;
                        g.time = time;
                       // System.out.println(server.getServerId() + ": updated " + localId +"'s heartbeat sequence to " + counter+" based on message from "+ g.id+" at node time " +System.currentTimeMillis());
                        log.log(Level.FINE, server.getServerId() + ": updated " + localId +"'s heartbeat sequence to " + counter+" based on message from "+ g.id+" at node time " +System.currentTimeMillis());
                    }

                }
                gossipInfoMap.put(localId,g);
                recievedGossip.add(g);

            }
        }
    }



    private class Deleter extends Thread {


        @Override
        public void run() {
            while(!shutdown) {
                deleteDeadServers();
            }

        }


        private void deleteDeadServers(){
            for (Long localId : gossipInfoMap.keySet()){
               // System.out.println("d"+ localId);
                Gossip gossip = gossipInfoMap.get(localId);
                Long time = System.currentTimeMillis();

                if ((time - gossip.time) >= FAIL){
                    //System.out.println("FAILED: " + localId);
                    //if(time - gossip.time < CLEANUP) {
                    System.out.println(server.getServerId() + ": no heartbeat from server " + localId + " -server failed");
                    log.log(Level.FINE, server.getServerId() + ": no heartbeat from server " + localId + " -server failed");

                    if (gossipInfoMap.containsKey(localId)) {
                            failed.add(localId);
                            if (!deleted.contains(localId)) delete(localId);

                        }
                }
            }
        }

        private void delete(long localId){

            if(localId==server.getServerId()) shutdown =true;

            try{
                sleep(FAIL);
            }catch (Exception e){
                System.out.println("EXCEPTION HERE");
            }


            gossipInfoMap.remove(localId);
            deleted.add(localId);


            Long leaderID = server.getCurrentLeader().getProposedLeaderID();
            ZooKeeperPeerServer.ServerState state = server.getPeerState();

            if (localId == leaderID){ //if the leader failed
                //delete Leader
                server.setPeerEpoch(server.getPeerEpoch()+1);
                if(state== ZooKeeperPeerServer.ServerState.FOLLOWING) {
                    server.setPeerState(ZooKeeperPeerServer.ServerState.LOOKING);
                    log.log(Level.FINE, server.getServerId()+": switching from " + state + " to LOOKING");
                    System.out.println(server.getServerId()+": switching from " + state + " to LOOKING");
                }
            } else{ //if the follower failed
                if(state == ZooKeeperPeerServer.ServerState.LEADING){ //if I am the leader, remove from roundRobin
                   server.roundRobinLeader.removeAFollower(localId);
              }else{
                    //not sure have to do anything else here

              }

            }


        }

    }


}
