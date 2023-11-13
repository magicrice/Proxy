package main.java.com.zky.server;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ServerProxy {
    private ServerToClient serverToClient;
    private OuterToServer outerToServer;

    public static void main(String[] args) throws IOException {
        new ServerProxy().run();
    }

    public void run(){
        serverToClient = new ServerToClient(this);
        outerToServer = new OuterToServer(this);

        serverToClient.start();
        outerToServer.start();
        try {
            serverToClient.join();
            outerToServer.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void sendToClient(InputStream inputStream, OutputStream outputStream)
            throws IOException, InterruptedException {
        serverToClient.sendToClient(inputStream, outputStream);
    }
}
