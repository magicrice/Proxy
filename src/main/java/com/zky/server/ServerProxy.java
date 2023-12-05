package com.zky.server;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerProxy {
    private Map<String, SocketChannel> clientSocketChannelMap = new ConcurrentHashMap<>();
    private String clientHost = "120.46.189.242";
    private Integer clientPort = 3306;
    private Integer limit = 10;

    public static void main(String[] args) throws Exception {
        new ServerProxy().run();
    }

    public void run() throws Exception {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(9999));
        Selector selector = Selector.open();

        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            int select = selector.select();
            if (select == 0) {
                Thread.sleep(100);
                continue;
            }
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey sk = iterator.next();
                if (sk.isAcceptable()) {
                    System.out.println("执行接受方法");
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    socketChannel.configureBlocking(false);
                    String uuid = UUID.randomUUID().toString();
                    socketChannel.register(selector, SelectionKey.OP_READ, uuid);

                    if (!clientSocketChannelMap.containsKey(uuid)) {
                        SocketChannel clientSocketChannel = SocketChannel.open(new InetSocketAddress(clientHost, clientPort));
                        clientSocketChannel.configureBlocking(false);
                        clientSocketChannelMap.put(uuid, clientSocketChannel);
                    }
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    int num = 0;
                    boolean flag = false;
                    while (num <= limit){
                        int read = clientSocketChannelMap.get(uuid).read(byteBuffer);
                        if(read == -1){
                            clientSocketChannelMap.remove(uuid);
                            break;
                        }else if(read == 0) {
                            if(flag){
                                num++;
                            }
                        }else {
                            flag = true;
                            byteBuffer.flip();
                            socketChannel.write(byteBuffer);
                            System.out.println(new String(byteBuffer.array()));
                            byteBuffer.clear();
                            num = 0;
                        }
                    }


                }else if (sk.isReadable()) {
                    System.out.println("执行读方法");
                    if (!clientSocketChannelMap.containsKey(sk.attachment().toString())) {
                        SocketChannel clientSocketChannel = SocketChannel.open(new InetSocketAddress(clientHost, clientPort));
                        clientSocketChannel.configureBlocking(false);
                        clientSocketChannelMap.put(sk.attachment().toString(), clientSocketChannel);
                    }
                    SocketChannel socketChannel = (SocketChannel) sk.channel();
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    int num = 0;
                    boolean flag = false;
                    while (num <= limit) {
                        int read = socketChannel.read(byteBuffer);
                        System.out.println(read);
                        if (read == -1) {
                            break;
                        } else if (read == 0) {
                            if(flag){
                                num++;
                            }
                        } else {
                            flag = true;
                            byteBuffer.flip();
                            clientSocketChannelMap.get(sk.attachment().toString()).write(byteBuffer);
                            System.out.println(new String(byteBuffer.array()));
                            byteBuffer.clear();
                            num = 0;
                        }
                    }
                    sk.interestOps(SelectionKey.OP_WRITE);
                }else if (sk.isWritable()) {
                    System.out.println("执行写方法");
                    if (!clientSocketChannelMap.containsKey(sk.attachment().toString())) {
                        SocketChannel clientSocketChannel = SocketChannel.open(new InetSocketAddress(clientHost, clientPort));
                        clientSocketChannel.configureBlocking(false);
                        clientSocketChannelMap.put(sk.attachment().toString(), clientSocketChannel);
                    }
                    SocketChannel socketChannel = (SocketChannel) sk.channel();
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    int num = 0;
                    boolean flag = false;
                    while (num <= limit) {
                        try {
                            int read = clientSocketChannelMap.get(sk.attachment().toString()).read(byteBuffer);
                            if (read == -1) {
                                break;
                            } else if (read == 0) {
                                if(flag){
                                    num++;
                                }
                            } else {
                                flag = true;
                                byteBuffer.flip();
                                socketChannel.write(byteBuffer);
                                System.out.println(new String(byteBuffer.array()));
                                byteBuffer.clear();
                                num = 0;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                    clientSocketChannelMap.get(sk.attachment().toString()).close();
                    clientSocketChannelMap.remove(sk.attachment().toString());
                    sk.cancel();
                }
                iterator.remove();
            }
        }
    }
}
