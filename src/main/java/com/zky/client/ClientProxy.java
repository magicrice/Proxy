package com.zky.client;

import java.io.*;
import java.net.Socket;
import java.util.Map;

public class ClientProxy {
//    Socket clientSocket;
    Socket cmdSocket;
    Socket intraSocket;
    Map<String,Socket> clientSocketMap;

    public static void main(String[] args) throws Exception {
        new ClientProxy().run();

    }

    public void run() throws Exception{
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        BufferedWriter bufferedWriter= null;
        while (true){
            try {
                int read = bufferedReader.read();
                if(read == -1){
                    break;
                }
                if(intraSocket == null || intraSocket.isClosed()){
                    intraSocket = new Socket("localhost",8080);
                    OutputStream outputStream = intraSocket.getOutputStream();
                    bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
                    new Intra().start();
                }
                System.out.print((char)read);
                bufferedWriter.write(read);
                bufferedWriter.flush();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }

    public class Intra extends Thread{
        @Override
        public void run() {
                try {
                    InputStream inputStream = intraSocket.getInputStream();
                    OutputStream outputStream = clientSocket.getOutputStream();
                    while (true){
                        int read = inputStream.read();
                        if(read == -1){
                            break;
                        }
                        System.out.print((char) read);
                        outputStream.write(read);
                    }
                    outputStream.flush();
                    intraSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        intraSocket.close();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            }
    }

    public class CmdClient extends Thread{
        @Override
        public void run() {
            try {
                cmdSocket = new Socket("localhost", 9099);
                InputStream inputStream = cmdSocket.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String s = bufferedReader.readLine();
                if(s.startsWith("CREATE")){
                    //请求创建通道
                    create(s);

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void create(String uuid){
        try {
            Socket newSocket = new Socket("localhost", 8088);
            clientSocketMap.put(uuid,newSocket);
            //向通道发送uuid
            OutputStream outputStream = newSocket.getOutputStream();
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
            bufferedWriter.write("CREATE-"+uuid);
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
