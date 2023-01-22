package edu.yu.cs.com3800.stage1;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.URI;
import java.net.http.HttpResponse;

public class ClientImpl implements Client {
    HttpClient client;
    URI uri;
    HttpResponse<String> response;

    public ClientImpl(String hostName, int hostPort) throws MalformedURLException {
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        this.uri =  URI.create("http://" + hostName + ":" + hostPort + "/compileandrun");
    }

    @Override
    public void sendCompileAndRunRequest(String src) throws IOException {

        if(src == null) throw new IllegalArgumentException();

        HttpRequest request =  HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(src))
                .uri(this.uri)
                .setHeader("Content-Type","text/x-java-source")
                .build();
        try {
            this.response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e){

        }
    }

    @Override
    public Response getResponse() throws IOException {
        Client.Response response = new Client.Response(this.response.statusCode(), this.response.body());
        return response;
    }




}
