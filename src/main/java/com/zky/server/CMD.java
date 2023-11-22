package com.zky.server;


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CMD extends Thread{

    private ServerSocket serverSocket;

    private ServerProxy serverProxy;

//    private Socket clientSocket;
    private Map<String,Socket> clientSocketMap = new ConcurrentHashMap<>();

    public CMD(ServerProxy serverProxy){
        this.serverProxy = serverProxy;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(9099);
            //获取客户端连接
            while (!serverSocket.isClosed()){
                Socket clientSocket = serverSocket.accept();
                System.out.println("cmd服务客户端已连接");
                new ClientCmd(clientSocket).start();
            }
            serverSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void close(){
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void create(String port,String uuid){
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(clientSocketMap.get(port).getOutputStream()));
            bufferedWriter.write("CREATE-"+uuid);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public class ClientCmd extends Thread{
        private Socket socket;
        private String port;
        public ClientCmd(Socket socket){
            this.socket = socket;
        }
        @Override
        public void run() {
            try {
                while (true){
                    InputStream inputStream = socket.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    String s = bufferedReader.readLine();
                    if(s.startsWith("PORT")){
                        String[] split = s.split("-");
                        port = split[1].replaceAll("\n", "").replaceAll("\r", "");
                        System.out.println("要建立的端口"+port);
                        if(clientSocketMap.containsKey(port)){
                            //端口已被占用
                            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                            bufferedWriter.write("ERROR-Port has be used");
                            bufferedWriter.newLine();
                            bufferedWriter.flush();
                        }else {
                            clientSocketMap.put(port,socket);
                            //创建外部服务
                            System.out.println("创建外部服务");
                            new Thread(()->{
                                serverProxy.createOuter(port);
                            }).start();
                        }
                    }
                }
            }catch (Exception e) {
                //断线，清除通道
                try {
                    socket.close();
                    clientSocketMap.remove(port);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
                serverProxy.clearSocket(port);
            }
        }
    }
}
