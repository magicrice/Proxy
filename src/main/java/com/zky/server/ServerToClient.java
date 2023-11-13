package com.zky.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerToClient extends Thread {

    private ServerSocket serverSocket;

    private Socket clientSocket;
    private OutputStream toOuter;
    private ServerProxy serverProxy;

    public ServerToClient(ServerProxy serverProxy) {
        this.serverProxy = serverProxy;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(8088);
            System.out.println("服务端已启动，端口：8088");
            new ToOuter().start();
            while (true) {
                clientSocket = serverSocket.accept();
                System.out.println("客户端连接:" + clientSocket.getLocalAddress());
//                 Thread.sleep(10);
                if (clientSocket != null) {
                    break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendToClient(InputStream is, OutputStream os) throws IOException {
        toOuter = os;
        OutputStream outputStream = clientSocket.getOutputStream();
        while (true) {
            try {
                int read = is.read();
                if (read == -1) {
                    break;
                }
                if (clientSocket != null) {
                    System.out.print((char) read);
                    outputStream.write(read);
                    outputStream.flush();
                }
            } catch (IOException e) {
                clientSocket.close();
                e.printStackTrace();
            }
        }
    }

    public void close(){
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class ToOuter extends Thread {

        @Override
        public void run() {
            while (true) {
                try {
                    if (clientSocket == null || clientSocket.isClosed()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        continue;
                    }
                    InputStream inputStream = clientSocket.getInputStream();
                    while (true) {
                        int read = inputStream.read();
                        System.out.print((char) read);
                        toOuter.write(read);
                        toOuter.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
