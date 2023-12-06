package com.zky.server;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerProxy {
    //    private Map<String, SocketChannel> clientSocketChannelMap = new ConcurrentHashMap<>();
    private String clientHost = "120.46.189.242";
    //    private String clientHost = "localhost";
    private Integer clientPort = 3306;
    //    private Integer clientPort = 8080;
    private Integer limit = 10;

    public static void main(String[] args) throws Exception {
        new ServerProxy().run();
    }

    public void run() throws Exception {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(9999));
        Selector acceptSelector = Selector.open();
        Selector readSelector = Selector.open();
        Selector writeSelector = Selector.open();

        serverSocketChannel.register(acceptSelector, SelectionKey.OP_ACCEPT);


        /**
         * acceptSelector
         */
        new Thread(() -> {
            try {
                while (true) {
                    int select = acceptSelector.select();
                    if (select == 0) {
                        Thread.sleep(100);
                        continue;
                    }
                    Set<SelectionKey> selectionKeys = acceptSelector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey sk = iterator.next();
                        if (sk.isAcceptable()) {
                            System.out.println("执行接受方法");
                            SocketChannel socketChannel = serverSocketChannel.accept();
                            socketChannel.configureBlocking(false);
                            String uuid = UUID.randomUUID().toString();
                            System.out.println("唯一id为：" + uuid);
                            socketChannel.register(writeSelector, SelectionKey.OP_WRITE, "out-" + uuid);
                            socketChannel.register(readSelector, SelectionKey.OP_READ, "out-" + uuid);
                            SocketChannel clientSocketChannel = SocketChannel.open(new InetSocketAddress(clientHost, clientPort));
                            clientSocketChannel.configureBlocking(false);
                            clientSocketChannel.register(writeSelector, SelectionKey.OP_WRITE, "client-" + uuid);
                            clientSocketChannel.register(readSelector, SelectionKey.OP_READ, "client-" + uuid);
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
                            String skAttachment = sk.attachment().toString();
                            SocketChannel clientSocketChannel = null;
                            if (sk.isReadable()) {
                                int i = writeSelector.selectNow();
                                if (i == 0) {
                                    Thread.sleep(100);
                                    continue;
                                }
                                Iterator<SelectionKey> clientIterator = writeSelector.selectedKeys().iterator();
                                if (!clientIterator.hasNext()) {
                                    continue;
                                }
                                while (clientIterator.hasNext()) {
                                    SelectionKey clientSelectionKey = clientIterator.next();
                                    if (skAttachment.startsWith("out")) {
                                        if (clientSelectionKey.attachment().toString().equals(skAttachment.replaceAll("out-", "client-"))) {
                                            clientSocketChannel = (SocketChannel) clientSelectionKey.channel();
                                            clientIterator.remove();
                                            flag = true;
                                        }
                                    }
                                    if (skAttachment.startsWith("client")) {
                                        if (clientSelectionKey.attachment().toString().equals(skAttachment.replaceAll("client-", "out-"))) {
                                            clientSocketChannel = (SocketChannel) clientSelectionKey.channel();
                                            clientIterator.remove();
                                            flag = true;
                                        }
                                    }
                                }
                                if(skAttachment.startsWith("out")){
                                    if(clientSocketChannel == null || !clientSocketChannel.isConnected()){
                                        clientSocketChannel = SocketChannel.open(new InetSocketAddress(clientHost, clientPort));
                                        clientSocketChannel.configureBlocking(false);
                                        clientSocketChannel.register(writeSelector, SelectionKey.OP_WRITE, "client-" + skAttachment.replaceAll("out-",""));
                                        clientSocketChannel.register(readSelector, SelectionKey.OP_READ, "client-" + skAttachment.replaceAll("out-",""));
                                        flag = true;
                                    }
                                }

                                System.out.println("执行读方法");
                                System.out.println(sk.attachment());
                                SocketChannel socketChannel = (SocketChannel) sk.channel();
                                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                                int num = 0;
                                try {
                                    while (num <= limit) {
                                        int read = socketChannel.read(byteBuffer);
                                        if (read == -1) {
                                            sk.cancel();
                                            break;
                                        } else if (read == 0) {
                                            num++;
                                        } else {
                                            byteBuffer.flip();
                                            //输出
                                            clientSocketChannel.write(byteBuffer);
                                            System.out.println(new String(byteBuffer.array()));
                                            byteBuffer.clear();
                                            num = 0;
                                        }
                                    }
                                }catch (Exception e){
                                    e.printStackTrace();
                                    sk.cancel();
                                }
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

        /**
         * writeSelector
         */
        new Thread(() -> {

        }).start();
    }

}
