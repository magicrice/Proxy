package com.zky.server;

import com.zky.basehandler.BaseServerSocketChannelHandler;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class OutServerSocketChannelHandler extends BaseServerSocketChannelHandler {
    @Override
    public void accept(SelectionKey sk) throws Exception {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) sk.channel();
        System.out.println("执行接受方法");
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        String uuid = UUID.randomUUID().toString();
        System.out.println("唯一id为：" + uuid);
        String port = sk.attachment().toString().replaceAll("out-", "").replaceAll("-", "");
        socketChannel.register(writeSelector, SelectionKey.OP_WRITE, sk.attachment().toString() + uuid);
        socketChannel.register(readSelector, SelectionKey.OP_READ, sk.attachment().toString() + uuid);
        //通过cmd向客户端发送连接通道消息
        while (true) {
            int i = writeSelector.selectNow();
            if (i == 0) {
                continue;
            }
            Set<SelectionKey> writeSelectorKeys = writeSelector.selectedKeys();
            Iterator<SelectionKey> writeIterators = writeSelectorKeys.iterator();
            boolean flag = false;
            while (writeIterators.hasNext()) {
                SelectionKey next = writeIterators.next();
                if (next.isWritable()) {
                    String s = next.attachment().toString();
                    if (s.startsWith("cmd-" + port)) {
                        SocketChannel cmdSocketChannel = (SocketChannel) next.channel();
                        cmdSocketChannel.write(ByteBuffer.wrap(("connect-" + port + "-" + uuid + "-end").getBytes(StandardCharsets.UTF_8)));
                        flag = true;
                    }
                }
                writeIterators.remove();
            }
            if (flag) {
                break;
            }
        }
    }

    @Override
    public void read(SelectionKey sk) throws Exception {
        SocketChannel clientSocketChannel = null;
        SocketChannel socketChannel = (SocketChannel) sk.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        String skAttachment = sk.attachment().toString();
        int i = writeSelector.selectNow();
        if (i == 0) {
            Thread.sleep(100);
            return;
        }
        Iterator<SelectionKey> clientIterator = writeSelector.selectedKeys().iterator();
        if (!clientIterator.hasNext()) {
            return;
        }
        while (clientIterator.hasNext()) {
            SelectionKey clientSelectionKey = clientIterator.next();
            if (clientSelectionKey.attachment().toString().equals(skAttachment.replaceAll("out-", "client-"))) {
                String uuid = skAttachment.replaceAll("out-", "");
                if (ServerProxy.serverClientChannelFlag.contains(uuid)) {
                    clientSocketChannel = (SocketChannel) clientSelectionKey.channel();
                }
            }
            clientIterator.remove();
        }
        if (clientSocketChannel == null || !clientSocketChannel.isConnected()) {
            System.out.println(skAttachment + "对应通道为空");
            Thread.sleep(100);
            return;
        }
        System.out.println("执行读方法");
        int num = 0;
        try {
            while (num <= ServerProxy.limit) {
                int read = socketChannel.read(byteBuffer);
                if (read == -1) {
                    sk.cancel();
                    //清除writeSelector的通道注册信息
                    System.out.println(writeSelector.selectNow());
                    Iterator<SelectionKey> tmpClientIterator = writeSelector.selectedKeys().iterator();
                    while (tmpClientIterator.hasNext()) {
                        SelectionKey next = tmpClientIterator.next();
                        String uuid = skAttachment.replaceAll("out-", "");
                        if (!"".equals(uuid)) {
                            if (next.attachment().toString().endsWith(uuid)) {
                                next.cancel();
                                next.channel().close();
                                System.out.println(next.attachment() + "通道关闭");
                            }
                        }
                        tmpClientIterator.remove();
                    }
                    System.out.println(skAttachment + "通道-1已关闭");
                    break;
                } else if (read == 0) {
                    num++;
                } else {
                    byteBuffer.flip();
                    String s = new String(byteBuffer.array());
                    System.out.println(s);
                    clientSocketChannel.write(byteBuffer);
                    System.out.println("向客户端发送数据完毕");
                    byteBuffer.clear();
                    num = 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            sk.cancel();
            //清除writeSelector的通道注册信息
            System.out.println(writeSelector.selectNow());
            Iterator<SelectionKey> tmpClientIterator = writeSelector.selectedKeys().iterator();
            while (tmpClientIterator.hasNext()) {
                SelectionKey next = tmpClientIterator.next();
                String uuid = skAttachment.replaceAll("out-", "");
                if (!"".equals(uuid)) {
                    if (next.attachment().toString().endsWith(uuid)) {
                        next.cancel();
                        next.channel().close();
                        System.out.println(next.attachment() + "通道关闭");
                    }
                }
                tmpClientIterator.remove();
            }
            System.out.println(skAttachment + "通道异常已关闭");
        }
    }

    @Override
    public void write() {

    }
}
