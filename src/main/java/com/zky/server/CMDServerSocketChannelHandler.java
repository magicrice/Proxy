package com.zky.server;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

public class CMDServerSocketChannelHandler extends BaseServerSocketChannelHandler {
    @Override
    public void accept(SelectionKey sk, Selector writeSelector, Selector readSelector) throws Exception {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) sk.channel();
        System.out.println("执行接受方法");
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(writeSelector, SelectionKey.OP_WRITE, "cmd-");
        socketChannel.register(readSelector, SelectionKey.OP_READ, "cmd-");
    }

    @Override
    public boolean read(SelectionKey sk, Selector writeSelector, Selector acceptSelector) throws Exception {
        boolean flag = false;
        SocketChannel socketChannel = (SocketChannel) sk.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        String skAttachment = sk.attachment().toString();
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
                        if (next.attachment().toString().startsWith("cmd-")) {
                            next.cancel();
                            next.channel().close();
                            System.out.println(next.attachment() + "通道关闭");
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
                    //输出
                    if (skAttachment.startsWith("cmd-")) {
                        if (s.startsWith("create")) {
                            s = s.substring(0, s.indexOf("-end"));
                            String[] split = s.split("-");
                            ServerSocketChannel outserversocketchannel = ServerSocketChannel.open();
                            outserversocketchannel.configureBlocking(false);
                            outserversocketchannel.bind(new InetSocketAddress(Integer.parseInt(split[1])));
                            outserversocketchannel.register(acceptSelector, SelectionKey.OP_ACCEPT, "out-");
                            System.out.println("对外服务创建成功");
                            flag = true;
                        }
                    }
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
                if (next.attachment().toString().startsWith("cmd-")) {
                    next.cancel();
                    next.channel().close();
                    System.out.println(next.attachment() + "通道关闭");
                }
                tmpClientIterator.remove();
            }
            System.out.println(skAttachment + "通道异常已关闭");
        }
        return flag;
    }

    @Override
    public void write() {

    }
}
