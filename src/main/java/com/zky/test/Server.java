package com.zky.test;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Server {
    private Socket outer;
    private Socket localhost;

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.run();
    }

    public void run(){
        try {
            new AgainThread().start();
            new AgainThread().start();
            new AgainThread().start();
            new AgainThread().start();
            new AgainThread().start();
            new AgainThread().start();
            new AgainThread().start();
            new AgainThread().start();
            new AgainThread().start();
            new AgainThread().start();
            new AgainThread().start();
            new AgainThread().start();
            ServerSocket serverSocket = new ServerSocket(9999);
            Socket accept = serverSocket.accept();
            InputStream inputStream = accept.getInputStream();
            int i = 0;
            while (true){
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                int read = inputStream.read();
                if(read == -1){
                    break;
                }
                System.out.println(read);
                i++;
            }
            System.out.println(i);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public class AgainThread extends Thread{
        @Override
        public void run() {
            System.out.println(Thread.currentThread().getName());
            while (true){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

//    public void run() throws IOException {
//        ServerSocket serverSocket = new ServerSocket(9999);
//        outer = serverSocket.accept();
//        localhost = new Socket("localhost", 8080);
//        new Outer().start();
//        InputStream inputStream = outer.getInputStream();
//        OutputStream outputStream = localhost.getOutputStream();
//        while (true){
//            int read = inputStream.read();
//            if(read == -1){
//                break;
//            }
//            outputStream.write(read);
//            outputStream.flush();
//        }
//    }

    public class Outer extends Thread {
        @Override
        public void run() {
            try {
                InputStream inputStream = localhost.getInputStream();
                OutputStream outputStream = outer.getOutputStream();
                while (true){
                    int read = inputStream.read();
                    if(read == -1){
                        break;
                    }
                    System.out.print((char) read);
                    outputStream.write(read);
                    outputStream.write(22);
                    outputStream.flush();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    }
}