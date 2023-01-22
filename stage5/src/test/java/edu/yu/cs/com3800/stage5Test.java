package edu.yu.cs.com3800;

import edu.yu.cs.com3800.stage1.ClientImpl;
import edu.yu.cs.com3800.stage5.GatewayPeerServerImpl;
import edu.yu.cs.com3800.stage5.GatewayServer;
import edu.yu.cs.com3800.stage5.ZooKeeperPeerServerImpl;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * TEST 1: Make sure that leader election works with observer
 *
 *
 */

public class stage5Test {
    private int[] ports;
    private int leaderPort;
    private int myPort;
    private InetSocketAddress myAddress;
    private ArrayList<ZooKeeperPeerServer> servers;
    private String validClass = "package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world!\";\n    }\n}\n";
    int countForGatewayPort= 0;
    GatewayPeerServerImpl gatewayPeerServer;
    ZooKeeperPeerServerImpl toShutdown;
    ZooKeeperPeerServer leader;
    /*
        Test for correct in queue/round robin
        that sending to correct servers
     */

    String basicCode =  "public class Test{ public String run(){ return \"Hello Server\";}}";
    String code2 =  "public class Test{ public String run(){ return \"Second Test :)\";}}";
    String code3 = "public class Test{ public String run(){ return \"yo yo yo - this is code 3\";}}";






    public void setup(int gatewayPort) {
        //start servers

        HashMap<Long, InetSocketAddress> peerIDtoAddress = new HashMap<>(8);
        for (int i = 0; i < this.ports.length; i++) {
            peerIDtoAddress.put(Integer.valueOf(i).longValue(), new InetSocketAddress("localhost", this.ports[i]));
        }


        GatewayServer gatewayServer = new GatewayServer(gatewayPort,99L, (Map<Long, InetSocketAddress>) peerIDtoAddress.clone());
        gatewayServer.start();

        GatewayPeerServerImpl gpsi = gatewayServer.getServer();
        // System.out.println(gpsi.getAddress().getPort());
        peerIDtoAddress.put(gpsi.getServerId(), new InetSocketAddress("localhost", gpsi.getAddress().getPort())); //adding the gatewaypeerserver
        new Thread(gpsi, "Server on port " + gpsi.getAddress().getPort()).start();

        this.servers = new ArrayList<>(3);
        this.servers.add(gpsi);
        this.gatewayPeerServer = gpsi;

        //create the rest of the servers.
        for (Map.Entry<Long, InetSocketAddress> entry : peerIDtoAddress.entrySet()) {
          //  System.out.println(entry.getKey());
            if(entry.getKey()==99) continue; //becasue already put in above
            HashMap<Long, InetSocketAddress> map = (HashMap<Long, InetSocketAddress>) peerIDtoAddress.clone();
            map.remove(entry.getKey());
            ZooKeeperPeerServerImpl server = new ZooKeeperPeerServerImpl(entry.getValue().getPort(), 0, entry.getKey(), map);
            if(entry.getKey() ==5)  toShutdown=server;
            leader = server; //should be the last one
            this.servers.add(server);
            new Thread(server, "Server on port " + server.getAddress().getPort()).start();
        }
       // System.out.println("toshutdown:"+ toShutdown.getServerId());

        try {
            Thread.sleep(2000);
        } catch (Exception e) {
        }


    /*

      //  printLeaders();
       // System.out.println("GateWay Leader: "+ gpsi.getCurrentLeader().getProposedLeaderID());

        try {
            Thread.sleep(3000);
        } catch (Exception e) {
        }

*/
    }

    private void printLeaders() {
        for (ZooKeeperPeerServer server : this.servers) {
            Vote leader = server.getCurrentLeader();
            if (leader != null) {
                System.out.println("Server on port " + server.getAddress().getPort() + " whose ID is " + server.getServerId() + " has the following ID as its leader: " + leader.getProposedLeaderID() + " and its state is " + server.getPeerState().name());
            }else{
                System.out.println("Null leader" + server.getServerId());
            }

        }
    }


    //@After
    public void shutdown() {
        for (ZooKeeperPeerServer server : this.servers) {
            server.shutdown();
        }
        this.gatewayPeerServer.shutdown();

    }



    @Test
    public void deleteServerID5(){

        this.ports = new int[]{8011, 8021, 8031, 8041, 8051, 8061, 8071, 8081};
        this.leaderPort = this.ports[this.ports.length - 1];
        this.myPort = 9902;
        this.myAddress = new InetSocketAddress("localhost", this.myPort);
        setup(9000);
        //System.out.println("HERE:!!"+toShutdown.getServerId());
        this.toShutdown.shutdown();
       //
        // System.out.println(this.toShutdown.getServerId());

        while(true){

        }

        //make sure that 5 is getting deleted by all the other servers
    }




    @Test
    public void deleteLeader(){
        this.ports = new int[]{8017, 8027, 8037, 8047, 8057, 8067, 8077, 8087};
        this.leaderPort = this.ports[this.ports.length - 1];
        this.myPort = 9992;
        this.myAddress = new InetSocketAddress("localhost", this.myPort);
        setup(8000);
        //System.out.println("HERE:!!"+toShutdown.getServerId());
        System.out.println(leader.getServerId());
        this.leader.shutdown();
        //
        // System.out.println(this.toShutdown.getServerId());

        while(true){

        }

        //make sure that 5 is getting deleted by all the other servers
    }

}