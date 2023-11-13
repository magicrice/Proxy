package com.zky.server;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ServerProxy {
    private ServerToClient serverToClient;
    private OuterToServer outerToServer;
    private CMD cmd;

    private boolean isClosed = false;

    public static void main(String[] args) throws IOException {
        ServerProxy serverProxy = new ServerProxy();
        serverProxy.run();
        Runtime.getRuntime().addShutdownHook(new Thread(()->serverProxy.close()));
    }

    public void run(){
        serverToClient = new ServerToClient(this);
        outerToServer = new OuterToServer(this);
        cmd = new CMD(this);

        serverToClient.start();
        outerToServer.start();
        cmd.start();
        try {
            serverToClient.join();
            outerToServer.join();
            cmd.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void sendToClient(InputStream inputStream, OutputStream outputStream)
            throws IOException, InterruptedException {
        serverToClient.sendToClient(inputStream, outputStream);
    }
    public void create(String uuid){
        cmd.create(uuid);
    }

    public void close(){
        serverToClient.close();
        outerToServer.close();
        cmd.close();
        isClosed = true;
    }
}