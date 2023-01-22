package edu.yu.cs.com3800.stage5;

import edu.yu.cs.com3800.ZooKeeperPeerServer;

import java.net.InetSocketAddress;
import java.util.Map;

public class GatewayPeerServerImpl extends ZooKeeperPeerServerImpl{


    public GatewayPeerServerImpl(int myPort, long peerEpoch, Long id, Map<Long, InetSocketAddress> peerIDtoAddress) {
        super(myPort, peerEpoch, id, peerIDtoAddress, ZooKeeperPeerServer.ServerState.OBSERVER);
    }

}
