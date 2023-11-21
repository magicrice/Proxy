package com.zky.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class OuterToServer {
    private ServerProxy serverProxy;
    private Map<String, ServerSocket> outerServerSocketMap = new ConcurrentHashMap<>();

    public OuterToServer(ServerProxy serverProxy) {
        this.serverProxy = serverProxy;
    }


    public void createOuter(String port) {
        System.out.println("线程idOuterToServer-->" + Thread.currentThread().getName() + "->" + Thread.currentThread().getId());
        try {
            ServerSocket serverSocket = new ServerSocket(Integer.parseInt(port));
            outerServerSocketMap.put(port,serverSocket);
            System.out.println(outerServerSocketMap.keySet());
            while (true) {
                Socket outerSocket = serverSocket.accept();
                System.out.println("接收到前端信息");

                String uuid = "";
                synchronized (this) {
                    uuid = serverProxy.lockSocket(port);
                    if (uuid == null || "".equals(uuid)) {
                        //发送client请求创建通道
                        uuid = port + UUID.randomUUID().toString();
                        serverProxy.create(port, uuid);
                        System.out.println("CMD服务发送给客户端指令创建连接");
                    }
                }
                try {
                    serverProxy.sendToClient(outerSocket, uuid);
                } catch (InterruptedException e) {
                    outerSocket.close();
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class OuterToServerBatch extends Thread {
        private String port;

        public OuterToServerBatch(String port) {
            this.port = port;
        }

        @Override
        public void run() {}
    }

}
