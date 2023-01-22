package edu.yu.cs.com3800;

import edu.yu.cs.com3800.stage3.ZooKeeperPeerServerImpl;
import jdk.jfr.Threshold;
import org.junit.*;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertTrue;


public class stage3Test {
    private int[] ports;
    private int leaderPort;
    private int myPort;
    private InetSocketAddress myAddress;
    private ArrayList<ZooKeeperPeerServer> servers;
    private String validClass = "package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world!\";\n    }\n}\n";


    /*
        Test for correct in queue/round robin
        that sending to correct servers
     */


    public void setup() {
        //start servers
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
            new Thread(server, "Server on port " + server.getAddress().getPort()).start();

        }

        try {
            Thread.sleep(3000);
        } catch (Exception e) {
        }

        printLeaders();

        try {
            Thread.sleep(3000);
        } catch (Exception e) {
        }


    }

    private void printLeaders() {
        for (ZooKeeperPeerServer server : this.servers) {
            Vote leader = server.getCurrentLeader();
            if (leader != null) {
                System.out.println("Server on port " + server.getAddress().getPort() + " whose ID is " + server.getServerId() + " has the following ID as its leader: " + leader.getProposedLeaderID() + " and its state is " + server.getPeerState().name());
            }

        }
    }


    @After
    public void shutdown() {
        for (ZooKeeperPeerServer server : this.servers) {
            server.shutdown();

        }
    }


    @Test
    public void JudahsDemo() throws Exception {
        this.ports = new int[]{8011, 8021, 8031, 8041, 8051, 8061, 8071, 8081};
        this.leaderPort = this.ports[this.ports.length - 1];
        this.myPort = 9999;
        this.myAddress = new InetSocketAddress("localhost", this.myPort);
        setup();
        JudahsDemo client1 = new JudahsDemo();
        client1.basicTest();
        client1.stopServers();
        client1.shutdown();
        shutdown();
    }



    @Test
    public void basicTestCustomClient() {
        this.ports = new int[]{8012, 8022, 8032, 8042, 8052, 8062, 8072, 8082};
        this.leaderPort = this.ports[this.ports.length - 1];
        this.myPort = 9992;
        this.myAddress = new InetSocketAddress("localhost", this.myPort);
        setup();



        ClientForTesting client = new ClientForTesting("localhost", this.myPort, "localhost", leaderPort);
        client.sendManyMessages(this.validClass, this.ports);
        try {
            client.printResponses();
        } catch (Exception e) {
            e.printStackTrace();
        }
        client.shutdown();
    }


    @Test
    public void badCode(){
        this.ports = new int[]{8017, 8027, 8037, 8047, 8057, 8067, 8077, 8087};
        this.leaderPort = this.ports[this.ports.length - 1];
        this.myPort = 9997;
        this.myAddress = new InetSocketAddress("localhost", this.myPort);
        setup();

        ClientForTesting client = new ClientForTesting("localhost", this.myPort, "localhost", leaderPort);
        client.sendManyMessages("this is bad code, it should not compile- the response should be the error", this.ports);
        try {
            client.printResponses();
        } catch (Exception e) {
            e.printStackTrace();
        }

        client.shutdown();

    }



   @Test
    public void multipleClients() {
        this.ports = new int[]{8013, 8023, 8033, 8043, 8053, 8063, 8073, 8083};
        this.leaderPort = this.ports[this.ports.length - 1];
        this.myPort = 9993;
        this.myAddress = new InetSocketAddress("localhost", this.myPort);
        setup();



        ClientForTesting client = new ClientForTesting("localhost", this.myPort, "localhost", leaderPort);
        ClientForTesting client2 = new ClientForTesting("localhost", 9994, "localhost", leaderPort );
        client.sendManyMessages(this.validClass, this.ports);
        String secondClientCode = "package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"From Client#2; Hello world!\";\n    }\n}\n";
        client2.sendManyMessages(secondClientCode, this.ports);
        try {
            client.printResponses();
            client2.printResponses();

        } catch (Exception e) {
            e.printStackTrace();
        }
        client.shutdown();
        client2.shutdown();
    }




}



