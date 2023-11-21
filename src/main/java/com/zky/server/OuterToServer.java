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
        System.out.println("线程idOuterToServer-->"+Thread.currentThread().getName()+"->"+Thread.currentThread().getId());
        try {
            outertServerSocket = new ServerSocket(7777);
            while (true){
                Socket outerSocket = outertServerSocket.accept();
                System.out.println("接收到前端信息");

                String uuid = "";
                synchronized (this){
                    uuid = serverProxy.lockSocket();
                    if(uuid == null || "".equals(uuid)){
                        //发送client请求创建通道
                        uuid = UUID.randomUUID().toString();
                        serverProxy.create(uuid);
                        System.out.println("CMD服务发送给客户端指令创建连接");
                    }
                }

                try {
                    serverProxy.sendToClient(outerSocket,uuid);
                } catch (InterruptedException e) {
                    outerSocket.close();
                    e.printStackTrace();
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
