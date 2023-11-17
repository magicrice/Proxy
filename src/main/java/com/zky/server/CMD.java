package com.zky.server;


import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class CMD extends Thread{

    private ServerSocket serverSocket;

    private ServerProxy serverProxy;

    private Socket clientSocket;

    public CMD(ServerProxy serverProxy){
        this.serverProxy = serverProxy;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(9099);
            //获取客户端连接
            while (!serverSocket.isClosed()){
                if(clientSocket == null || clientSocket.isClosed()){
                    clientSocket = serverSocket.accept();
                    System.out.println("cmd服务客户端已连接");
                }
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

    public void create(String uuid){
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            bufferedWriter.write("CREATE-"+uuid);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
