package com.zky.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerToClient extends Thread {

    private ServerSocket serverSocket;

//    private Socket clientSocket;
    private Map<String,Socket> clientSocketMap = new ConcurrentHashMap<>();
    private Map<String,Socket> toOuterMap = new ConcurrentHashMap<>();
    private ServerProxy serverProxy;

    public ServerToClient(ServerProxy serverProxy) {
        this.serverProxy = serverProxy;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(8088);
            System.out.println("服务端已启动，端口：8088");
            while (true) {
                Socket accept = serverSocket.accept();
                new ToOuter(accept).start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendToClient(Socket socket,String uuid) throws IOException {
        while (!clientSocketMap.containsKey(uuid) ){
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        toOuterMap.put(uuid, socket);
        System.out.println("目前存在服务端连接请求端通道数量"+toOuterMap.size()+"-->"+toOuterMap.keySet());
        new SendToClient(uuid).start();
    }

    public void close(){
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class ToOuter extends Thread {
        private Socket socket;
        public ToOuter(Socket socket){
            this.socket = socket;
        }

        @Override
        public void run() {
            System.out.println("线程idToOuter-->"+Thread.currentThread().getName()+"->"+Thread.currentThread().getId());
            InputStream inputStream = null;
            try {
                inputStream = socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            StringBuilder msg = new StringBuilder();
            OutputStream outputStream = null;
            String uuid = "";
            boolean isConnect = false;
            while (true){
                int read = 0;
                try {
                    read = inputStream.read();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
                if(read == -1){
                    break;
                }
                msg.append((char) read);
                if(msg.toString().startsWith("CREATE")&& msg.toString().endsWith("\n")){
                    //绑定新客户端
                    uuid = msg.toString().replaceAll("CREATE-", "").replaceAll("\n","").replace("\r","");
                    System.out.println("收到创建信息存储通道:"+uuid);
                    clientSocketMap.put(uuid,socket);
                    System.out.println("目前存在服务端连接客户端通道数量"+clientSocketMap.size()+"-->"+clientSocketMap.keySet());
                    msg = new StringBuilder();
                    isConnect = true;
                }

                if(isConnect){
                    if(outputStream== null){
                        try {
                            while (!toOuterMap.containsKey(uuid)){
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            outputStream = toOuterMap.get(uuid).getOutputStream();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        outputStream.write(read);
                        outputStream.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        }
    }


    public class SendToClient extends Thread{
        private String uuid;
        public SendToClient(String uuid){
            this.uuid = uuid;
        }
        @Override
        public void run() {
            System.out.println("线程idSendToClient-->"+Thread.currentThread().getName()+"->"+Thread.currentThread().getId());
            while (!toOuterMap.containsKey(uuid)){
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("发送给客户端前端请求");
            OutputStream outputStream = null;
            InputStream inputStream = null;
            try {
                inputStream = toOuterMap.get(uuid).getInputStream();
                outputStream = clientSocketMap.get(uuid).getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (true) {
                try {
                    int read = inputStream.read();
                    if (read == -1) {
                        break;
                    }
                    if (clientSocketMap.get(uuid) != null) {
                        outputStream.write(read);
                        outputStream.flush();
                    }
                } catch (IOException e) {
                    try {
                        clientSocketMap.get(uuid).close();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                    e.printStackTrace();
                    break;
                }
            }
        }
    }
}
