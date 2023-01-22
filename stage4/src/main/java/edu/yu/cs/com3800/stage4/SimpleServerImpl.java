package edu.yu.cs.com3800.stage4;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import edu.yu.cs.com3800.JavaRunner;
import edu.yu.cs.com3800.Message;
import edu.yu.cs.com3800.SimpleServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.sql.SQLOutput;
import java.util.Calendar;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.sun.net.httpserver.HttpHandler;

public class SimpleServerImpl extends Thread implements SimpleServer{
    HttpServer server;
    static Logger log = Logger.getLogger(SimpleServerImpl.class.getName());
    static FileHandler fileHandler;

    public SimpleServerImpl(int port) throws IOException {
        this.server= HttpServer.create(new InetSocketAddress(port), 0);
        this.fileHandler = new FileHandler("ServerLog" + Calendar.getInstance().getTime().toString().replaceAll(":", "-") + ".log");
        this.log.addHandler(fileHandler);
        SimpleFormatter formatter = new SimpleFormatter();
        fileHandler.setFormatter(formatter);
    }
    @Override
    public void start() {
        this.log.info("Server Started at " + Calendar.getInstance().getTime().toString().replaceAll(":", "-"));
        this.server.createContext("/compileandrun", new Handler());
        this.server.setExecutor(null); // creates a default executor
        this.server.start();
    }


    /*@Override
    public void stop() {
          this.server.stop(2);
        this.log.info("Server Stopped at " + Calendar.getInstance().getTime().toString().replaceAll(":", "-"));
    }*/

    class Handler implements HttpHandler{

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] buffer = exchange.getRequestBody().readAllBytes();
            String stringBody = new String(buffer);
            log.info("Java Code Input: " + stringBody);
            InputStream stream = new ByteArrayInputStream(buffer);
            JavaRunner jr = new JavaRunner();
            String response = "";


            if(exchange.getRequestMethod().equals("GET")){
                exchange.sendResponseHeaders(405, 0);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                log.info("Status Code: " + 405);
                log.info("Error: GET Requests are Not Valid");
                log.info("Output:       ");
                os.close();
                exchange.close();
            }

            if(!exchange.getRequestHeaders().containsKey("Content-Type") || !(exchange.getRequestHeaders().get("Content-Type").get(0).equals("text/x-java-source"))){
                exchange.sendResponseHeaders(400, 0);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                log.info("Status Code: " + 400);
                log.info("Error: No Proper Content-Type Key-Mapping");
                log.info("Output:       ");
                os.close();
                exchange.close();

            } else{

                try { //todo this is where the work gets done!!!
                //    Message message = new Message(Message.MessageType.WORK,buffer,"localhost", );
                    // response = jr.compileAndRun(stream);
                    // stream.close();
                    //exchange.sendResponseHeaders(200, response.length());
                    //OutputStream os = exchange.getResponseBody();
                    //os.write(response.getBytes());
                    // log.info("Status Code: " + 200);
                    //log.info("Output: " + new String(response.getBytes()));
                    //os.close();


                    exchange.close();

                } catch (Exception e) { //do better here
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    e.printStackTrace(new PrintStream (bytes));
                    String stackTrace = bytes.toString();
                    response = e.getMessage() +"\n" + stackTrace ;
                    exchange.sendResponseHeaders(400, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    log.info("Status Code: " + 400);
                    log.info("Output: " + new String(response.getBytes()));
                    os.close();
                    exchange.close();
                }
            }



        }
    }

}
