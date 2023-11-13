package com.zky.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;

public class OuterToServer extends Thread{
    private ServerProxy serverProxy;
    private ServerSocket outertServerSocket;

    public OuterToServer(ServerProxy serverProxy){
        this.serverProxy = serverProxy;
    }

    @Override
    public void run() {
        try {
            outertServerSocket = new ServerSocket(7777);
            while (true){
                Socket outerSocket = outertServerSocket.accept();


                //发送client请求创建通道
                String uuid = UUID.randomUUID().toString();
                serverProxy.create(uuid);

                InputStream inputStream = outerSocket.getInputStream();
                OutputStream outputStream = outerSocket.getOutputStream();
                try {
                    serverProxy.sendToClient(inputStream,outputStream);
                    outerSocket.close();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    outerSocket.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void close(){
        try {
            outertServerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
