package com.zky.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class ServerToClient extends Thread {

    private ServerSocket serverSocket;

    private Map<String,Socket> clientSocketMap = new ConcurrentHashMap<>();
    private Set<String> uuidSet = new CopyOnWriteArraySet<>();
    private Map<String,Socket> toOuterMap = new ConcurrentHashMap<>();
    private ServerProxy serverProxy;

    public ServerToClient(ServerProxy serverProxy) {
        this.serverProxy = serverProxy;
    }
    public String lockSocket(String port){
        AtomicReference<String> uuid = new AtomicReference<>("");
        clientSocketMap.forEach((a,b)->{
            if (!uuidSet.contains(a) && a.startsWith(port)) {
                uuid.set(a);
                return;
            }
        });
        Set<String> strings = clientSocketMap.keySet();
        Set<String> collect = strings.stream().filter(a -> !uuidSet.contains(a)).skip(5).collect(Collectors.toSet());
        for (String s : collect) {
            try {
                clientSocketMap.get(s).close();
                clientSocketMap.remove(s);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        uuidSet.add(uuid.get());
        return uuid.get();
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

    public void sendToClient(Socket socket,String uuid) {
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
            clientSocketMap.forEach((a,b)->{
                try {
                    b.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
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
                    System.out.println("流关闭");
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
                    continue;
                }

                if(isConnect){
//                    if(outputStream== null){
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
//                    }
                    try {
                        System.out.print((char) read);
                        outputStream.write(read);
                        outputStream.flush();
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
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
                        System.out.println("前端流关闭");
                        toOuterMap.remove(uuid);
                        uuidSet.remove(uuid);
                        //关闭客户端服务端流
                        break;
                    }
                    if (outputStream != null) {
                        System.out.print((char) read);
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
