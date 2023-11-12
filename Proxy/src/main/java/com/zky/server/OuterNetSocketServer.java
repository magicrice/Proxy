package com.zky.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 监听外网请求类
 */
public class OuterNetSocketServer extends Thread {

    private ServerProxy serverProxy;

    private ServerSocket outerNetServerSocket;

    public OuterNetSocketServer(ServerProxy serverProxy) {
        this.serverProxy = serverProxy;
    }

    @Override
    public void run() {

        try {

            // 创建ServerSocket并监听本地端口
            // 转发外网来的流量
            outerNetServerSocket = new ServerSocket(7777);
            System.out.println("OuterNetSocketServer started on port 7777");

            while (!serverProxy.isClose()) {

                // 等待客户端连接
                Socket clientSocket = outerNetServerSocket.accept();
                System.out.println("OuterNetSocketServer Accepted connection from " + clientSocket.getInetAddress().getHostAddress());

                // 设置读超时
                // 当这份代码在远程环境跑的时候 多报几次错误之后
                // 即使tcp连接断开了 读InputStream的线程也会阻塞在read
//                clientSocket.setSoTimeout(5000);
//                clientSocket.setSoTimeout(-1);

                try {
                    serverProxy.outToIntra(clientSocket.getInputStream(), clientSocket.getOutputStream());
                    clientSocket.close();
                } catch (Exception e) {
                    System.out.println("出问题了也继续执行");
                    clientSocket.close();
                    e.printStackTrace();
                }

            }

            outerNetServerSocket.close();
            System.out.println("OuterNetSocketServer close");

        } catch (IOException e){

            e.printStackTrace();
        }

    }

    public void close() {
        try {
            outerNetServerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
