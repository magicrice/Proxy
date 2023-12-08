package com.zky.server;


import com.zky.client.BaseClientSocketChannelHandler;

import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerProxy {
    public static Integer limit = 2;
    public static Map<String, BaseServerSocketChannelHandler> handlerMap = new ConcurrentHashMap<>();
    static {
        handlerMap.put("client",new ClientServerSocketChannelHandler());
        handlerMap.put("out",new OutServerSocketChannelHandler());
        handlerMap.put("cmd",new CMDServerSocketChannelHandler());
    }

    public static void main(String[] args) throws Exception {
        new ServerProxy().run();
    }

    public void run() throws Exception {

        Selector acceptSelector = Selector.open();
        Selector readSelector = Selector.open();
        Selector writeSelector = Selector.open();

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
                            socketChannelHandler.accept(sk,writeSelector,readSelector);
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
            try {
                while (true) {
                    int select = readSelector.selectNow();
                    if (select == 0) {
                        Thread.sleep(100);
                        continue;
                    }
                    while (true) {
                        Set<SelectionKey> selectionKeys = readSelector.selectedKeys();
                        if (selectionKeys.size() == 0) {
                            break;
                        }
                        Iterator<SelectionKey> iterator = selectionKeys.iterator();
                        while (iterator.hasNext()) {
                            boolean flag = false;
                            SelectionKey sk = iterator.next();
                            if (sk.isReadable()) {
                                BaseServerSocketChannelHandler socketChannelHandler = handlerMap.get(sk.attachment().toString().substring(0, sk.attachment().toString().indexOf("-")));
                                flag = socketChannelHandler.read(sk,writeSelector,acceptSelector);
                            }
                            if (flag) {
                                iterator.remove();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

}
