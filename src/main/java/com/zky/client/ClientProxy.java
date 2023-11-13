package com.zky.client;

import java.io.*;
import java.net.Socket;

public class ClientProxy {
    Socket clientSocket;
    Socket intraSocket;

    public static void main(String[] args) throws Exception {
        new ClientProxy().run();

    }

    public void run() throws Exception{
        clientSocket = new Socket("localhost", 8088);
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
}
