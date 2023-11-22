package com.zky.server;


import java.io.IOException;
import java.net.Socket;

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
        cmd.start();
        try {
            serverToClient.join();
            cmd.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void sendToClient(Socket socket, String uuid)
            throws IOException, InterruptedException {
        serverToClient.sendToClient(socket,uuid);
    }
    public void create(String port,String uuid){
        cmd.create(port,uuid);
    }
//    public String lockSocket(String port){
//        return serverToClient.lockSocket(port);
//    }

    public void createOuter(String port) {
        outerToServer.createOuter(port);
    }

    public void close(){
        serverToClient.close();
        cmd.close();
        isClosed = true;
    }

}
