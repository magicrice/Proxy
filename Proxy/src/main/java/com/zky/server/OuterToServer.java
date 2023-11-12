package com.zky.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class OuterToServer extends Thread{
    private ServerProxy serverProxy;

    public OuterToServer(ServerProxy serverProxy){
        this.serverProxy = serverProxy;
    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(7777);
            while (true){
                Socket outerSocket = serverSocket.accept();
                InputStream inputStream = outerSocket.getInputStream();
                OutputStream outputStream = outerSocket.getOutputStream();
                serverProxy.sendToClient(inputStream,outputStream);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
