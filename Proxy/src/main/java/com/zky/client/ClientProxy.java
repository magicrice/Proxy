package com.zky.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ClientProxy {
    Socket clientSocket;
    Socket intraSocket;

    public static void main(String[] args) throws Exception {
        new ClientProxy().run();

    }

    public void run() throws Exception{
        clientSocket = new Socket("localhost", 8088);
        InputStream inputStream = clientSocket.getInputStream();
        intraSocket = new Socket("localhost", 8080);
        OutputStream outputStream = intraSocket.getOutputStream();
        new Intra().start();
        while (true){
            int read = inputStream.read();
            if(read == -1){
                break;
            }
            System.out.print((char)read);
            outputStream.write(read);
            outputStream.flush();
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
                    outputStream.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
