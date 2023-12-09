package com.zky.server;



import com.zky.basehandler.BaseServerSocketChannelHandler;
import com.zky.context.ServerSelectorContext;

import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

public class ServerProxy extends ServerSelectorContext {
    public static Map<String, BaseServerSocketChannelHandler> handlerMap = new HashMap<>();

    static {
        handlerMap.put("client", new ClientServerSocketChannelHandler());
        handlerMap.put("out", new OutServerSocketChannelHandler());
        handlerMap.put("cmd", new CMDServerSocketChannelHandler());
    }

    public ServerProxy() {
        start();
    }

    public static void main(String[] args) throws Exception {
        new ServerProxy().run();
    }

    public void run() throws Exception {

        /**
         * 与客户端交互服务
         */
        ServerSocketChannel clientServerSocketChannel = ServerSocketChannel.open();
        clientServerSocketChannel.configureBlocking(false);
        clientServerSocketChannel.bind(new InetSocketAddress(8088));

        /**
         * cmd服务
         */
        ServerSocketChannel cmdServerSocketChannel = ServerSocketChannel.open();
        cmdServerSocketChannel.configureBlocking(false);
        cmdServerSocketChannel.bind(new InetSocketAddress(9099));


        clientServerSocketChannel.register(acceptSelector, SelectionKey.OP_ACCEPT, "client-");
        cmdServerSocketChannel.register(acceptSelector, SelectionKey.OP_ACCEPT, "cmd-");


        /**
         * acceptSelector
         */
        new Thread(() -> {
            try {
                while (true) {
                    int select = acceptSelector.selectNow();
                    if (select == 0) {
                        Thread.sleep(100);
                        continue;
                    }
                    Set<SelectionKey> selectionKeys = acceptSelector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey sk = iterator.next();
                        if (sk.isAcceptable()) {
                            BaseServerSocketChannelHandler socketChannelHandler = handlerMap.get(sk.attachment().toString().substring(0, sk.attachment().toString().indexOf("-")));
                            socketChannelHandler.accept(sk);
                        }
                        iterator.remove();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        /**
         * readSelector
         */
        new Thread(() -> {
            while (true) {
                try {
                    int select = readSelector.selectNow();
                    if (select == 0) {
                        Thread.sleep(100);
                        continue;
                    }

                    Set<SelectionKey> selectionKeys = readSelector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey sk = iterator.next();
                        if (sk.isReadable()) {
                            BaseServerSocketChannelHandler socketChannelHandler = handlerMap.get(sk.attachment().toString().substring(0, sk.attachment().toString().indexOf("-")));
                            socketChannelHandler.read(sk);
                        }
                        iterator.remove();
                    }
            } catch (Exception e) {
                e.printStackTrace();
            }
            }
        }).start();
    }

}
