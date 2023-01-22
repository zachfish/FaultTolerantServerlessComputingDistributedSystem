package edu.yu.cs.com3800.stage4;

import edu.yu.cs.com3800.Message;
import edu.yu.cs.com3800.Vote;
import edu.yu.cs.com3800.ZooKeeperPeerServer;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

public class GatewayPeerServerImpl extends ZooKeeperPeerServerImpl{


    public GatewayPeerServerImpl(int myPort, long peerEpoch, Long id, Map<Long, InetSocketAddress> peerIDtoAddress) {
        super(myPort, peerEpoch, id, peerIDtoAddress, ZooKeeperPeerServer.ServerState.OBSERVER);
    }

}
