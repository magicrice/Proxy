package com.zky.client;


import com.zky.basehandler.BaseClientSocketChannelHandler;
import com.zky.context.ClientSelectorContext;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientProxy extends ClientSelectorContext {

    public static Map<String, BaseClientSocketChannelHandler> handlerMap = new HashMap<>();
    public static List<String> reflectionLists = new CopyOnWriteArrayList<>();

    static {
        reflectionLists.add("120.46.189.242:3306->9999");
        reflectionLists.add("localhost:8080->7777");

        handlerMap.put("server", new ServerSocketChannelHandler());
        handlerMap.put("beRepresented", new BeRepresentedSocketChannelHandler());
        handlerMap.put("cmd", new CMDSocketChannelHandler());
    }

    public ClientProxy() {
        start(reflectionLists);
    }

    public static void main(String[] args) throws Exception {
        new ClientProxy().run();
    }

    public void run() throws Exception {

        /**
         * client请求server创建cmd通道
         */
        new Thread(() -> {
            while (true) {
                reflections.forEach((a, b) -> {
                    try {
                        if (!b.getFlag()) {
                            SocketChannel cmdSocketChannel = SocketChannel.open(new InetSocketAddress("localhost", 9099));
                            cmdSocketChannel.configureBlocking(false);
                            System.out.println("请求创建对外服务");
                            cmdSocketChannel.write(ByteBuffer.wrap(("create-" + b.getOutPort() + "-end").getBytes(StandardCharsets.UTF_8)));
                            cmdSocketChannel.register(cmdCreateSelector, SelectionKey.OP_READ, "cmd-" + a);
                            b.setFlag(true);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        }).start();

        //cmdSelector
        new Thread(() -> {
            while (true) {
                try {
                    int select = cmdCreateSelector.selectNow();
                    if (select == 0) {
                        Thread.sleep(100);
                        continue;
                    }
                    Set<SelectionKey> selectionKeys = cmdCreateSelector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey sk = iterator.next();
                        if (sk.isReadable()) {
                            handlerMap.get("cmd").read(sk);
                        }
                        iterator.remove();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
                    Set<SelectionKey> selectionKeys = readSelector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey sk = iterator.next();
                        if (sk.isReadable()) {
                            BaseClientSocketChannelHandler socketChannelHandler = handlerMap.get(sk.attachment().toString().substring(0, sk.attachment().toString().indexOf("-")));
                            socketChannelHandler.read(sk);
                        }
                        iterator.remove();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

    }

}
