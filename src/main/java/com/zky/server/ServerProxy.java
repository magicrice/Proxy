package com.zky.server;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerProxy {
    private Map<String, SocketChannel> clientSocketChannelMap = new ConcurrentHashMap<>();
        private String clientHost = "120.46.189.242";
//    private String clientHost = "localhost";
        private Integer clientPort = 3306;
//    private Integer clientPort = 8080;
    private Integer limit = 10000;

    public static void main(String[] args) throws Exception {
        new ServerProxy().run();
    }

    public void run() throws Exception {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(9999));
        Selector outerSelector = Selector.open();

        serverSocketChannel.register(outerSelector, SelectionKey.OP_ACCEPT);


        while (true) {
            int select = outerSelector.select();
            if (select == 0) {
                Thread.sleep(100);
                continue;
            }
            Set<SelectionKey> selectionKeys = outerSelector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey sk = iterator.next();
                if (sk.isAcceptable()) {
                    System.out.println("执行接受方法");
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    socketChannel.configureBlocking(false);
                    String uuid = UUID.randomUUID().toString();
                    System.out.println("唯一id为：" + uuid);
                    socketChannel.register(outerSelector, SelectionKey.OP_WRITE, uuid);
                } else if (sk.isReadable()) {
                    System.out.println("执行读方法");
                    if (!clientSocketChannelMap.containsKey(sk.attachment().toString())) {
                        SocketChannel clientSocketChannel = SocketChannel.open(new InetSocketAddress(clientHost, clientPort));
                        clientSocketChannel.configureBlocking(false);
                        clientSocketChannelMap.put(sk.attachment().toString(), clientSocketChannel);
                    }
                    System.out.println(sk.attachment());
                    SocketChannel socketChannel = (SocketChannel) sk.channel();
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    int num = 0;
                    while (num <= limit) {
                        int read = socketChannel.read(byteBuffer);
                        if (read == -1) {
                            sk.cancel();
                            clientSocketChannelMap.get(sk.attachment().toString()).close();
                            clientSocketChannelMap.remove(sk.attachment().toString());
                            break;
                        } else if (read == 0) {
                            num++;
                        } else {
                            byteBuffer.flip();
                            clientSocketChannelMap.get(sk.attachment().toString()).write(byteBuffer);
                            System.out.println(new String(byteBuffer.array()));
                            byteBuffer.clear();
                            num = 0;
                        }
                    }
                    if(sk.isValid()){
                        sk.interestOps(SelectionKey.OP_WRITE);
                    }
                } else if (sk.isWritable()) {
                    System.out.println("执行写方法");
                    if (!clientSocketChannelMap.containsKey(sk.attachment().toString())) {
                        SocketChannel clientSocketChannel = SocketChannel.open(new InetSocketAddress(clientHost, clientPort));
                        clientSocketChannel.configureBlocking(false);
                        clientSocketChannelMap.put(sk.attachment().toString(), clientSocketChannel);
                    }
                    System.out.println(sk.attachment());
                    SocketChannel socketChannel = (SocketChannel) sk.channel();
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    int num = 0;
                    while (num <= limit) {
                        try {
                            int read = clientSocketChannelMap.get(sk.attachment().toString()).read(byteBuffer);
                            if (read == -1) {
                                clientSocketChannelMap.get(sk.attachment().toString()).close();
                                clientSocketChannelMap.remove(sk.attachment().toString());
                                break;
                            } else if (read == 0) {
                                num++;
                            } else {
                                byteBuffer.flip();
                                socketChannel.write(byteBuffer);
                                System.out.println(new String(byteBuffer.array()));
                                byteBuffer.clear();
                                num = 0;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            clientSocketChannelMap.get(sk.attachment().toString()).close();
                            clientSocketChannelMap.remove(sk.attachment().toString());
                            break;
                        }
                    }
                    sk.interestOps(SelectionKey.OP_READ);
                }
                iterator.remove();
            }
        }
    }
}
