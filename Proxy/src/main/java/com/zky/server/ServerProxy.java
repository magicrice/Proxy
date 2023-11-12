package com.zky.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Server-Proxy启动类
 */
public class ServerProxy {

    private IntranetSocketServer intranetSocket;

    private OuterNetSocketServer outerNetSocket;

    private boolean close = false;

    public static void main(String[] args) {

        ServerProxy serverProxy = new ServerProxy();
        serverProxy.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> serverProxy.close()));
    }

    public void start() {
        intranetSocket = new IntranetSocketServer(this);
        outerNetSocket = new OuterNetSocketServer(this);

        intranetSocket.start();
        outerNetSocket.start();

        try {
            intranetSocket.join();
            outerNetSocket.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void outToIntra(InputStream inputStream, OutputStream outputStream)
            throws IOException, InterruptedException {
        intranetSocket.transferTo(inputStream, outputStream);
    }

    public boolean isClose() {
        return close;
    }

    public void close() {
        this.close = true;
        intranetSocket.close();
        outerNetSocket.close();
    }

}
