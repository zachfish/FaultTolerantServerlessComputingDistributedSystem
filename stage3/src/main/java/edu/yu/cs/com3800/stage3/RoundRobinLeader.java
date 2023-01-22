package edu.yu.cs.com3800.stage3;

import edu.yu.cs.com3800.Message;

import java.net.InetSocketAddress;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class RoundRobinLeader extends Thread {
    private int leaderPort;
    private Map<Long, InetSocketAddress> peerIDtoAddress;
    private LinkedBlockingQueue<InetSocketAddress> queue;
    private LinkedBlockingQueue<Message> outgoingMessages;
    private LinkedBlockingQueue<Message> incomingMessages;
    private InetSocketAddress server;
    private Long requestId =0L;
    //private boolean shutdown = false;
  //  private InetSocketAddress client;
    HashMap<Long, InetSocketAddress> clientMap = new HashMap<>(); //key: worker -> values: original client


    public RoundRobinLeader(Map<Long, InetSocketAddress> peerIDtoAddress,LinkedBlockingQueue<Message> incomingMessages, LinkedBlockingQueue<Message> outgoingMessages,InetSocketAddress server){
        setDaemon(true);
        queue = new LinkedBlockingQueue(peerIDtoAddress.values());
        this.incomingMessages = incomingMessages;
        this.outgoingMessages = outgoingMessages;
        this.server = server;
    }

    public void shutdown() {
        interrupt();
    }



    @Override
    public void run() {
        while(!this.isInterrupted()) {
            Message message = incomingMessages.poll();
            if(message!= null) {

                if (message.getMessageType().equals(Message.MessageType.WORK)){
                    requestId++;
                  //  this.client = new InetSocketAddress(message.getSenderHost(), message.getSenderPort());
                    this.clientMap.put(requestId, new InetSocketAddress(message.getSenderHost(), message.getSenderPort()));
                    Message messageWithRequestId = new Message(message.getMessageType(), message.getMessageContents(), message.getSenderHost(), message.getSenderPort(), message.getReceiverHost(), message.getReceiverPort(), this.requestId);
                    sendToWorker(messageWithRequestId);
                }
                else if(message.getMessageType().equals(Message.MessageType.COMPLETED_WORK)){
                    sendToClient(message);
                }

            }

        }


    }


    private void sendToWorker(Message message){
        InetSocketAddress worker = this.queue.poll();
        Message newMessage = new Message(Message.MessageType.WORK,message.getMessageContents(), this.server.getHostString(), this.server.getPort(), worker.getHostString(),worker.getPort(), message.getRequestID());
        this.outgoingMessages.offer(newMessage);
        this.queue.offer(worker);
    }

    private void sendToClient(Message message){
        InetSocketAddress client = clientMap.get(message.getRequestID());
        Message newMessage = new Message(Message.MessageType.COMPLETED_WORK,message.getMessageContents(),message.getSenderHost(),message.getSenderPort(), client.getHostName(), client.getPort());
        this.outgoingMessages.offer(newMessage);
    }


}
