package edu.yu.cs.com3800.stage5;

public class Gossip {
    protected int senderPort;
    protected long counter;
    protected long time;
    protected long id;
    protected boolean alive;



    public Gossip(long time, long counter, long id){
       // this.senderPort = senderPort;
        this.counter= counter;
        this.time =time;
        this.id = id;
    }
   /*
    public Gossip(int senderPort, long time, long counter, long id){
        this.senderPort = senderPort;
        this.counter= counter;
        this.time =time;
        this.id = id;
    }
*/


}
