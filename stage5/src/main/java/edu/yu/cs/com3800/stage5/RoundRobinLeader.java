package edu.yu.cs.com3800.stage5;
import edu.yu.cs.com3800.LoggingServer;
import edu.yu.cs.com3800.Message;
import edu.yu.cs.com3800.Util;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;
import java.util.logging.SocketHandler;

public class RoundRobinLeader extends Thread implements LoggingServer {
    private int leaderPort;
    private Map<Long, InetSocketAddress> peerIDtoAddress;
    private LinkedBlockingQueue<InetSocketAddress> queue;
    private LinkedBlockingQueue<Message> outgoingMessages;
    private LinkedBlockingQueue<Message> incomingMessages;
    LinkedBlockingQueue<Message> messagesFromClient;
    private InetSocketAddress server;
    private Long requestId =0L;
    HashMap<Long, InetSocketAddress> clientMap = new HashMap<>(); //key: worker -> values: original client
    private SocketHandler handler;
    ServerSocket serverSocket;
    Logger logger;




    public RoundRobinLeader(Map<Long, InetSocketAddress> peerIDtoAddress, LinkedBlockingQueue<Message> incomingMessages, LinkedBlockingQueue<Message> outgoingMessages, InetSocketAddress server){
        this.setName("RoundRobinLeader-port"+server.getPort());
        this.logger=initializeLogging(RoundRobinLeader.class.getCanonicalName()+"-port-"+server.getPort());
        setDaemon(true);
        queue = new LinkedBlockingQueue(peerIDtoAddress.values());

        try {
            this.serverSocket = new ServerSocket(server.getPort()+3);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(server.getPort());

        }
        this.server = server;
    }

    public void shutdown() {
        interrupt();

    }



    @Override
    public void run() {
        this.logger.info("Round Robin Started on Port: "+this.server.getPort());
        while(!this.isInterrupted()) {

            Socket socketAccepted = null;
            try {

                socketAccepted = serverSocket.accept();
                byte[] data = Util.readAllBytesFromNetwork(socketAccepted.getInputStream());
                Message message = new Message(data);


                if (message.getMessageType().equals(Message.MessageType.WORK)) {
                    this.logger.fine("Received Message from GateWay:\n"+message.toString());
                    removeGateWayFromQueue(message.getSenderPort());
                    this.requestId++;
                    this.clientMap.put(requestId, new InetSocketAddress(message.getSenderHost(), message.getSenderPort()));
                   // message = new Message(Message.MessageType.WORK, data., this.server.getHostString(), this.server.getPort(), this.server.getHostString(), this.server.getPort(), requestId); //will be changed.
                    sendToWorker(message); //todo have to make sure not to send to gateway for work!!

                }

                else if(message.getMessageType().equals(Message.MessageType.COMPLETED_WORK)){
                    this.logger.fine("Got Message back from Worker :\n"+message.toString());
                    sendToGateway(message);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }



    private void removeGateWayFromQueue(int port){
        for (InetSocketAddress a : this.queue){
            if (a.getPort() ==port){
                this.queue.remove(a);
            }
        }
    }


    private void sendToGateway(Message message){

        InetSocketAddress gatewayPeerServer = this.clientMap.get(message.getRequestID());
        //System.out.println("port: " +gatewayPeerServer.getPort()+2);
       Socket socket = null;
        try {
            socket = new Socket(gatewayPeerServer.getHostName(), gatewayPeerServer.getPort()+2);
            OutputStream output = socket.getOutputStream();
            output.write(message.getNetworkPayload());
            this.logger.fine("Sent Response to Gateway");
        } catch (IOException e) {
            //e.printStackTrace(System.out);
            this.logger.severe("Error sending to Gateway");
            System.err.println(e.getMessage());

        }


    }


    private void sendToWorker(Message message){
        InetSocketAddress worker = this.queue.poll();

        Message newMessage = new Message(Message.MessageType.WORK,message. getMessageContents(), this.server.getHostString(), this.server.getPort(), worker.getHostString(),worker.getPort(), this.requestId);



        ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(4); //todo - how many threads !!

        Thread tcpServerSendThread = new Thread(() -> {
            Socket socket = null;
            try {
                socket = new Socket(worker.getHostName(), worker.getPort() + 2);
                OutputStream output = socket.getOutputStream();
                output.write(newMessage.getNetworkPayload());
                this.logger.fine("Sent work to worker");
            } catch (IOException e) {
                //e.printStackTrace(System.out);
                this.logger.severe("Error sending to Worker");
                System.err.println(e.getMessage());

            }

        });

        threadPool.execute(tcpServerSendThread);
        //tcpServerSendThread.start();
        this.queue.offer(worker);
    }


    protected void removeAFollower(Long id){
        InetSocketAddress address = peerIDtoAddress.get(id);
        for(InetSocketAddress i : queue){
            if(i.equals(address)){
                queue.remove(i);
            }
        }
    }







}
