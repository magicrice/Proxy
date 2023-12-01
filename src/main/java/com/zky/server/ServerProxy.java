package com.zky.server;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerProxy {
    private Map<String,SocketChannel> clientSocketChannelMap = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        new ServerProxy().run();
    }
    public void run() throws Exception{
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
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    socketChannel.configureBlocking(false);
                    socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE,UUID.randomUUID().toString());

                } else if (sk.isReadable()) {
                    SocketChannel clientSocketChannel = SocketChannel.open(new InetSocketAddress("localhost", 8080));
                    clientSocketChannelMap.put(sk.attachment().toString(),clientSocketChannel);
                    SocketChannel socketChannel = (SocketChannel) sk.channel();
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    int num = 0;
                    while (num <= 10) {
                        int read = socketChannel.read(byteBuffer);
                        if (read == -1) {
                            break;
                        } else if (read == 0) {
                            num++;
                        } else {
                            byteBuffer.flip();
                            clientSocketChannel.write(byteBuffer);
                            System.out.println(new String(byteBuffer.array()));
                            byteBuffer.clear();
                            num = 0;
                        }
                    }
                    sk.interestOps(SelectionKey.OP_WRITE);
                } else if (sk.isWritable()) {
                    if(clientSocketChannelMap.containsKey(sk.attachment())){
                        SocketChannel socketChannel = (SocketChannel) sk.channel();
                        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                        int num = 0;
                        while (num <= 10) {
                            try {
                                int read = clientSocketChannelMap.get(sk.attachment().toString()).read(byteBuffer);
                                if (read == -1) {
                                    break;
                                }else if(read == 0){
                                    num++;
                                } else {
                                    byteBuffer.flip();
                                    socketChannel.write(byteBuffer);
                                    System.out.println(new String(byteBuffer.array()));
                                    byteBuffer.clear();
                                    num = 0;
                                }
                            } catch (Exception e) {
                                break;
                            }
                        }
                        clientSocketChannelMap.get(sk.attachment().toString()).close();
                        clientSocketChannelMap.remove(sk.attachment().toString());
                        sk.cancel();
                    }
                }
                iterator.remove();
            }
        }
    }
}
