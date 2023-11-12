package com.zky.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 监听内网客户端上线类
 *
 */
public class IntranetSocketServer extends Thread {

    private ServerProxy serverProxy;

    private ServerSocket intranetServerSocket;

    private Socket intranetClientSocket;

    private OutputStream outerNetOutputStream;

    public IntranetSocketServer(ServerProxy serverProxy) {
        this.serverProxy = serverProxy;
    }

    @Override
    public void run() {
        try {
            // 创建ServerSocket并监听本地端口
            // 等待内网的服务上线连接此端口
            // 公网服务器与内网服务器建立一条长连接 这样公网服务器才能找到内网服务器
            intranetServerSocket = new ServerSocket(8888);
            System.out.println("IntranetServerSocket started on port 8888");

            new IntraToOutThread().start();

            while (!serverProxy.isClose()) {

                if (intranetClientSocket == null || intranetClientSocket.isClosed()) {

                    // 等待客户端连接
                    intranetClientSocket = intranetServerSocket.accept();
                    System.out.println("IntranetServerSocket Accepted connection from " +
                            intranetClientSocket.getInetAddress().getHostAddress());
                }

                Thread.sleep(1000);
            }

            intranetServerSocket.close();
            System.out.println("IntranetServerSocket close");

        } catch (IOException e){

            e.printStackTrace();
        } catch (InterruptedException e) {

            e.printStackTrace();
        }
    }

    public void transferTo(InputStream outerNetInputStream, OutputStream outerNetOutputStream)
            throws IOException, InterruptedException {

        while (intranetClientSocket == null || intranetClientSocket.isClosed()) {
            System.out.println("服务器未上线");
            Thread.sleep(1000);
        }

        this.outerNetOutputStream = outerNetOutputStream;

        OutputStream outputStream = intranetClientSocket.getOutputStream();
        while (true) {
            int read = outerNetInputStream.read();
            if (-1 == read) {
                break ;
            }
            System.out.print((char) read);
            try {
                outputStream.write(read);
                outputStream.flush();
            } catch (Exception e) {
                intranetClientSocket.close();
                throw e;
            }

        }

        System.out.println();
        System.out.println();
        System.out.println("outerNet传输结束");
        System.out.println();
        System.out.println();

    }

    public void close() {
        try {
            intranetClientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * tcp是一条全双工通道，可以同时进行数据的发送和接受
     * 所以外网发数据给内网和内网发数据给外网是同时进行，是两条线程
     *
     * 我们不可能用一条线程，读完所有外网的数据之后再去读内网的数据，
     * 而且有可能外网发送完数据之后不会告诉我们数据发送完毕，而是等到收到响应之后在告诉我们数据发送完毕，
     * 如果用一条线程，就会阻塞在读外网的read方法
     *
     * 以Http为例，
     * 浏览器发送请求
     * Server-Proxy一边读数据一边写数据给内网
     * 读完完整的request之后阻塞在read方法
     * 本地服务器接收到完整request之后开始处理 并返回response
     * Server-Proxy的另一个线程把这个response返回给浏览器
     * 浏览器收到完整的response告诉Server-Proxy数据发送完毕
     * 阻塞在read方法的线程重新允许
     * 结束
     *
     * IntraToOutThread是内网给外网写数据的线程
     * 并且这个线程只能允许一条
     *
     * 之前这个线程我是在transferTo方法里去start的
     * 线程里读的是Server-Proxy与Client-Proxy这条长连接的数据，而且是死循环
     * 因为这条连接不能断开，所以read方法不会返回-1，线程永远不会终止，没有数据的时候就阻塞在read方法
     * 所以第二次调用的时候会有两条线程在竞争数据导致数据缺失
     * 但只要transferTo方法收到了外网写数据完毕的事件之后，强行把IntraToOutThread线程关了，下次进来就不会有两条了
     * 最坑的就是这里，因为IntraToOutThread最终都会阻塞在read方法，虽然线程被强制停止了，一旦有数据，还是会被这条已死的线程读一次，导致数据不完整
     *
     */
    class IntraToOutThread extends Thread {

        @Override
        public void run() {

            while (!serverProxy.isClose()) {

                try {

                    if (null == intranetClientSocket || intranetClientSocket.isClosed()) {
                        Thread.sleep(500);
                        continue ;
                    }

                    int read = -1;
                    try {
                        read = intranetClientSocket.getInputStream().read();
                    } catch (Exception e) {
                        intranetClientSocket.close();
                        throw e;
                    }

                    System.out.print((char) read);
                    outerNetOutputStream.write(read);
                    outerNetOutputStream.flush();

                } catch (Exception e) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    e.printStackTrace();
                }

            }

        }

    }

}
