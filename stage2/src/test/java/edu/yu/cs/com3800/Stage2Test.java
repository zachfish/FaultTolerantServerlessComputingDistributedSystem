package edu.yu.cs.com3800;
import edu.yu.cs.com3800.stage2.ZooKeeperPeerServerImpl;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertEquals;


public class Stage2Test {



    @Test //Checks for correct leader, server states, quroum size.
    public void JudahsDemo(){
        HashMap<Long, InetSocketAddress> peerIDtoAddress = new HashMap<>(3);
        peerIDtoAddress.put(1L, new InetSocketAddress("localhost", 8010));
        peerIDtoAddress.put(2L, new InetSocketAddress("localhost", 8020));
        peerIDtoAddress.put(3L, new InetSocketAddress("localhost", 8030));
        peerIDtoAddress.put(4L, new InetSocketAddress("localhost", 8040));
        peerIDtoAddress.put(5L, new InetSocketAddress("localhost", 8050));
        peerIDtoAddress.put(6L, new InetSocketAddress("localhost", 8060));
        peerIDtoAddress.put(7L, new InetSocketAddress("localhost", 8070));
        peerIDtoAddress.put(8L, new InetSocketAddress("localhost", 8080));

        //create servers
        ArrayList<ZooKeeperPeerServer> servers = new ArrayList<>(3);
        for (Map.Entry<Long, InetSocketAddress> entry : peerIDtoAddress.entrySet()) {
            HashMap<Long, InetSocketAddress> map = (HashMap<Long, InetSocketAddress>) peerIDtoAddress.clone();
            map.remove(entry.getKey());
            ZooKeeperPeerServerImpl server = new ZooKeeperPeerServerImpl(entry.getValue().getPort(), 0, entry.getKey(), map);
            servers.add(server);
            new Thread(server, "Server on port " + server.getAddress().getPort()).start();
        }
        //wait for threads to start
        try {
            Thread.sleep(10000);
        }
        catch (Exception e) {
        }

        //print out the leaders and shutdown
        for (ZooKeeperPeerServer server : servers) {
            assertEquals(5, server.getQuorumSize());
            Vote leader = server.getCurrentLeader();
            if (leader != null) {
                System.out.println("Server on port " + server.getAddress().getPort() + " whose ID is " + server.getServerId() + " has the following ID as its leader: " + leader.getProposedLeaderID() + " and its state is " + server.getPeerState().name());
                assertEquals(8, leader.getProposedLeaderID());
                if(server.getServerId()!=8){
                    assertEquals(ZooKeeperPeerServer.ServerState.FOLLOWING, server.getPeerState());
                } else{
                    assertEquals(ZooKeeperPeerServer.ServerState.LEADING, server.getPeerState());
                }
                server.shutdown();
            }
        }



}



   @Test
    public void BiggerDemo30(){

       HashMap<Long, InetSocketAddress> peerIDtoAddress = new HashMap<>(3);

        for (int x=9; x <39; x++){
            peerIDtoAddress.put((long) x,new InetSocketAddress("localhost",8000+(x*10)));
        }


        //create servers
        ArrayList<ZooKeeperPeerServer> servers = new ArrayList<>(3);
        for (Map.Entry<Long, InetSocketAddress> entry : peerIDtoAddress.entrySet()) {
            HashMap<Long, InetSocketAddress> map = (HashMap<Long, InetSocketAddress>) peerIDtoAddress.clone();
            map.remove(entry.getKey());
            ZooKeeperPeerServerImpl server = new ZooKeeperPeerServerImpl(entry.getValue().getPort(), 0, entry.getKey(), map);
            servers.add(server);
            new Thread(server, "Server on port " + server.getAddress().getPort()).start();
        }
        //wait for threads to start
        try {
            Thread.sleep(20000);
        }
        catch (Exception e) {
        }

        //print out the leaders and shutdown
        for (ZooKeeperPeerServer server : servers) {
            assertEquals(16, server.getQuorumSize());
            Vote leader = server.getCurrentLeader();
            if (leader != null) {
                System.out.println("Server on port " + server.getAddress().getPort() + " whose ID is " + server.getServerId() + " has the following ID as its leader: " + leader.getProposedLeaderID() + " and its state is " + server.getPeerState().name());
                assertEquals(38, leader.getProposedLeaderID());
                if(server.getServerId()!=38){
                    assertEquals(ZooKeeperPeerServer.ServerState.FOLLOWING, server.getPeerState());
                } else{
                    assertEquals(ZooKeeperPeerServer.ServerState.LEADING, server.getPeerState());
                }
                server.shutdown();
            }
        }



    }



    //The added server should accept the elected server as its leader, even though the new server has the higher id
    @Test
    public void addedAfterElection(){
        HashMap<Long, InetSocketAddress> peerIDtoAddress = new HashMap<>(3);
        peerIDtoAddress.put(1L, new InetSocketAddress("localhost", 8010));
        peerIDtoAddress.put(2L, new InetSocketAddress("localhost", 8020));
        peerIDtoAddress.put(3L, new InetSocketAddress("localhost", 8030));
        peerIDtoAddress.put(4L, new InetSocketAddress("localhost", 8040));
        peerIDtoAddress.put(5L, new InetSocketAddress("localhost", 8050));
        peerIDtoAddress.put(6L, new InetSocketAddress("localhost", 8060));
        peerIDtoAddress.put(7L, new InetSocketAddress("localhost", 8070));
        peerIDtoAddress.put(8L, new InetSocketAddress("localhost", 8080));

        //create servers
        ArrayList<ZooKeeperPeerServer> servers = new ArrayList<>(3);
        for (Map.Entry<Long, InetSocketAddress> entry : peerIDtoAddress.entrySet()) {
            HashMap<Long, InetSocketAddress> map = (HashMap<Long, InetSocketAddress>) peerIDtoAddress.clone();
            map.remove(entry.getKey());
            ZooKeeperPeerServerImpl server = new ZooKeeperPeerServerImpl(entry.getValue().getPort(), 0, entry.getKey(), map);
            servers.add(server);
            new Thread(server, "Server on port " + server.getAddress().getPort()).start();
        }
        //wait for threads to start
        try {
            Thread.sleep(10000);
        }
        catch (Exception e) {
        }

        //print out the leaders and shutdown
        for (ZooKeeperPeerServer server : servers) {
            Vote leader = server.getCurrentLeader();
            if (leader != null) {
                System.out.println("Server on port " + server.getAddress().getPort() + " whose ID is " + server.getServerId() + " has the following ID as its leader: " + leader.getProposedLeaderID() + " and its state is " + server.getPeerState().name());
                server.shutdown();
            }
        }


        ZooKeeperPeerServerImpl newServer = new ZooKeeperPeerServerImpl(8090,0, 9L, (Map<Long, InetSocketAddress>) peerIDtoAddress.clone());
        servers.add(newServer);
        new Thread(newServer, "Server on port " + newServer.getAddress().getPort()).start();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Server on port " + newServer.getAddress().getPort() + " whose ID is " + newServer.getServerId() + " has the following ID as its leader: " + newServer.getCurrentLeader().getProposedLeaderID() + " and its state is " + newServer.getPeerState().name());
        newServer.shutdown();


    }












}
